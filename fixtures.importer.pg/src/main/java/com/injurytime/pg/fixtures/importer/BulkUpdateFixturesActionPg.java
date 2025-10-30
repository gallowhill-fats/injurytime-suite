/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.pg.fixtures.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.injurytime.storage.api.AppConfig;
import com.injurytime.storage.api.AppConfigExt;
import com.injurytime.storage.api.JpaAccess;
import com.injurytime.storage.api.AppConfigExt;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;

@ActionID(category = "Tools", id = "com.injurytime.pg.tools.fixtures.BulkUpdateFixturesActionPg")
@ActionRegistration(displayName = "#CTL_BulkUpdateFixtures")
@ActionReference(path = "Menu/Tools", position = 1765)
@Messages(
        {
            "CTL_BulkUpdateFixtures=Fetch/Upsert Fixtures (CSV of league_ids)",
            // --- Parameter docs for MSG_BulkUpdateFixtures_Done ---
            "# {0} - number of leagues scanned",
            "# {1} - number of HTTP calls made",
            "# {2} - number of fixture rows upserted",
            "# {3} - number of team rows upserted",
            "# {4} - number of errors",
            "# {5} - remaining daily requests (string or '?')",
            "# {6} - daily request limit (string or '?')",
            // ------------------------------------------------------

            "MSG_BulkUpdateFixtures_Done=Leagues scanned: {0}, HTTP calls: {1}, fixtures upserted: {2}, teams upserted: {3}.\nErrors: {4}.\nRemaining daily calls (if known): {5} of {6}."
        })
public final class BulkUpdateFixturesActionPg implements ActionListener {

    private static final RequestProcessor RP = new RequestProcessor(BulkUpdateFixturesActionPg.class);

    private final JpaAccess jpa = Lookup.getDefault().lookup(JpaAccess.class);
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // Config keys (override in ~/injurytime.local.properties)
    private static final String KEY_SEASON = "injurytime.season.current";
    private static final String KEY_HOST = "RAPID_HOST";
    private static final String KEY_API_KEY = "RAPID_KEY";

    // Default values (can be overridden by props)
    private static final String DEFAULT_HOST = "api-football-v1.p.rapidapi.com";

    // Soft/Hard guard rails for daily limit (headers)
    private static final int WARN_REMAINING = AppConfigExt.getInt("RAPID_WARN_REMAINING", 250);
    private static final int HARD_STOP = AppConfigExt.getInt("RAPID_HARD_STOP", 25);
    
    final Logger log = Logger.getLogger(BulkUpdateFixturesActionPg.class.getName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (jpa == null)
        {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("No JPA available.", NotifyDescriptor.WARNING_MESSAGE));
            return;
        }

        // Pick the CSV with league_id,league_code
        FileChooserBuilder fcb = new FileChooserBuilder("fixtures-leagues-csv");
        File start = new File(AppConfigExt.getOr("chooser.dir", System.getProperty("user.home")));
        File csv = fcb.setTitle("Select leagues CSV (league_id,league_code)")
                .setDefaultWorkingDirectory(start)
                .setFilesOnly(true)
                .showOpenDialog();

        if (csv == null)
        {
            return;
        }

        final int season = AppConfigExt.getInt(KEY_SEASON, Instant.now().atZone(java.time.ZoneOffset.UTC).getYear());
        final String rapidHost = AppConfigExt.getOr(KEY_HOST, DEFAULT_HOST);
        final String rapidKey = AppConfig.get(KEY_API_KEY);
        if (rapidKey == null || rapidKey.isBlank())
        {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Missing RapidAPI key (rapid.key).", NotifyDescriptor.ERROR_MESSAGE));
            return;
        }

        RP.post(() -> run(csv, season, rapidHost, rapidKey));
    }

    private void run(File csv, int season, String rapidHost, String rapidKey)
    {
        List<Integer> leagueIds = readLeagueIds(csv);
        if (leagueIds.isEmpty())
        {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("No league_ids found in CSV.", NotifyDescriptor.WARNING_MESSAGE));
            return;
        }

        final AtomicInteger leaguesScanned = new AtomicInteger();
        final AtomicInteger httpCalls = new AtomicInteger();
        final AtomicInteger fixturesUp = new AtomicInteger();
        final AtomicInteger teamsUp = new AtomicInteger();
        final AtomicInteger errors = new AtomicInteger();
        final Integer[] lastLimit =
        {
            null
        };
        final Integer[] lastRemaining =
        {
            null
        };
        final boolean[] warned =
        {
            false
        };

        ProgressHandle ph = ProgressHandleFactory.createHandle("Fetching fixtures");
ph.start(leagueIds.size());

try { // <-- OUTER try that pairs with the final 'finally { ph.finish(); }'
  for (int idx = 0; idx < leagueIds.size(); idx++) {
    final int leagueId = leagueIds.get(idx);
    ph.progress("League " + leagueId + " / " + season + " (" + (idx + 1) + "/" + leagueIds.size() + ")", idx);

    try { // per-league try/catch
      HttpResponse<String> res = fetchOneLeague(leagueId, season, rapidHost, rapidKey);
      httpCalls.incrementAndGet();

      // headers, limits...
      res.headers().firstValue("x-ratelimit-requests-limit").ifPresent(v -> { try { lastLimit[0] = Integer.parseInt(v.trim()); } catch (Exception ignore) {} });
      res.headers().firstValue("x-ratelimit-requests-remaining").ifPresent(v -> { try { lastRemaining[0] = Integer.parseInt(v.trim()); } catch (Exception ignore) {} });

      int sc = res.statusCode();
      if (sc == 429) { errors.incrementAndGet(); break; }
      if (sc / 100 != 2) { errors.incrementAndGet(); continue; }

      if (lastRemaining[0] != null) {
        if (!warned[0] && lastRemaining[0] <= WARN_REMAINING) {
          log.warning("[FixturesBulk] Warning: only " + lastRemaining[0] + " requests remaining out of " + (lastLimit[0] != null ? lastLimit[0] : "?"));
          warned[0] = true;
        }
        if (lastRemaining[0] <= HARD_STOP) break;
      }

      String body = res.body();
      JsonNode root = mapper.readTree(body);
      JsonNode respNode = root.path("response");
      if (!respNode.isArray()) respNode = mapper.createArrayNode();
      final JsonNode respArr = respNode;   // effectively final for the lambda

      final int[] fIns = {0};
      final int[] tIns = {0};

      jpa.tx(em -> {
        for (JsonNode item : respArr) {
          Integer fixtureId = null; // visible to inner catch
          try {
            // --- extract minimal fields first ---
            fixtureId = getIntOrNull(item.path("fixture"), "id");
            if (fixtureId == null) {
              log.warning("Skipping item with null fixture.id");
              continue;
            }
            Integer lidPayload   = getIntOrNull(item.path("league"), "id");
            Integer seasonPayload= getIntOrNull(item.path("league"), "season");
            String  round        = nullIfBlank(item.path("league").path("round").asText(null));
            String  statusS      = nullIfBlank(item.path("fixture").path("status").path("short").asText(null));
            String  dateStr      = nullIfBlank(item.path("fixture").path("date").asText(null));
            Instant kickUtc      = parseIsoInstant(dateStr);
            Timestamp matchTs    = (kickUtc == null ? null : Timestamp.from(kickUtc));
            Timestamp importedAt = Timestamp.from(Instant.now());

            JsonNode teams = item.path("teams");
            Integer homeId   = getIntOrNull(teams.path("home"), "id");
            Integer awayId   = getIntOrNull(teams.path("away"), "id");
            String  homeName = nullIfBlank(teams.path("home").path("name").asText(null));
            String  awayName = nullIfBlank(teams.path("away").path("name").asText(null));

            JsonNode score = item.path("score");
            Integer htH = getIntOrNull(score.path("halftime"), "home");
            Integer htA = getIntOrNull(score.path("halftime"), "away");
            Integer ftH = getIntOrNull(score.path("fulltime"), "home");
            Integer ftA = getIntOrNull(score.path("fulltime"), "away");

            // upsert fixture
            em.createNativeQuery("""
              INSERT INTO fixture (fixture_id, league_id, season, round_name, status_short, match_date_utc, imported_at)
              VALUES (:fid, :lid, :season, :round, :st, :koff, :imported_at)
              ON CONFLICT (fixture_id) DO UPDATE SET
                league_id      = EXCLUDED.league_id,
                season         = EXCLUDED.season,
                round_name     = EXCLUDED.round_name,
                status_short   = EXCLUDED.status_short,
                match_date_utc = EXCLUDED.match_date_utc,
                imported_at    = EXCLUDED.imported_at
            """)
              .setParameter("fid", fixtureId)
              .setParameter("lid", (lidPayload != null ? lidPayload : leagueId))
              .setParameter("season", (seasonPayload != null ? seasonPayload : season))
              .setParameter("round", round)
              .setParameter("st", statusS)
              .setParameter("koff", matchTs)
              .setParameter("imported_at", importedAt)
              .executeUpdate();
            fIns[0]++;

            // upsert fixture_teams (match your schema)
            em.createNativeQuery("""
              INSERT INTO fixture_teams (
                fixture_id,
                home_team_id, home_team,
                away_team_id, away_team,
                ht_home_goals, ht_away_goals,
                ft_home_goals, ft_away_goals
              ) VALUES (
                :fid,
                :hid, :hname,
                :aid, :aname,
                :hth, :hta,
                :fth, :fta
              )
              ON CONFLICT (fixture_id) DO UPDATE SET
                home_team_id  = EXCLUDED.home_team_id,
                home_team     = EXCLUDED.home_team,
                away_team_id  = EXCLUDED.away_team_id,
                away_team     = EXCLUDED.away_team,
                ht_home_goals = EXCLUDED.ht_home_goals,
                ht_away_goals = EXCLUDED.ht_away_goals,
                ft_home_goals = EXCLUDED.ft_home_goals,
                ft_away_goals = EXCLUDED.ft_away_goals
            """)
              .setParameter("fid", fixtureId)
              .setParameter("hid", homeId)
              .setParameter("hname", homeName)
              .setParameter("aid", awayId)
              .setParameter("aname", awayName)
              .setParameter("hth", htH)
              .setParameter("hta", htA)
              .setParameter("fth", ftH)
              .setParameter("fta", ftA)
              .executeUpdate();
            tIns[0]++;

          } catch (Exception ex) {
            errors.incrementAndGet();
            log.log(Level.SEVERE,
                "Per-fixture upsert failed (league={0}, season={1}, fixtureId={2})",
                new Object[]{leagueId, season, fixtureId});
            log.log(Level.SEVERE, "Cause:", ex);
          }
        }
        return null;
      });

      // per-league counters & pacing
      fixturesUp.addAndGet(fIns[0]);
      teamsUp.addAndGet(tIns[0]);
      leaguesScanned.incrementAndGet();
      Thread.sleep(120L);

    } catch (Exception ex) { // per-league catch (don't reference fixtureId here)
      errors.incrementAndGet();
      log.log(Level.SEVERE,
          "Bulk fixtures upsert failed for league={0}, season={1}",
          new Object[]{leagueId, season});
      log.log(Level.SEVERE, "Cause:", ex);
    }
  } // end for
} finally { // <-- pairs with the outer try
  ph.finish();
}


        // Summary
        String rem = (lastRemaining[0] == null ? "?" : String.valueOf(lastRemaining[0]));
        String lim = (lastLimit[0] == null ? "?" : String.valueOf(lastLimit[0]));

        String msg = Bundle.MSG_BulkUpdateFixtures_Done(
                leaguesScanned.get(),
                httpCalls.get(),
                fixturesUp.get(),
                teamsUp.get(),
                errors.get(),
                rem,
                lim
        );
        DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(msg, NotifyDescriptor.PLAIN_MESSAGE));

    }

    // --- helpers ---
    private static List<Integer> readLeagueIds(File csv)
    {
        List<Integer> out = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(csv.toPath(), StandardCharsets.UTF_8))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                String s = line.strip();
                if (s.isEmpty() || s.startsWith("#"))
                {
                    continue;
                }
                // league_id,league_code  (we ignore the code)
                String[] parts = s.split(",", 2);
                try
                {
                    int id = Integer.parseInt(parts[0].trim());
                    out.add(id);
                } catch (Exception ignore)
                {
                }
            }
        } catch (IOException e)
        {
            // let caller show summary error count
        }
        return out;
    }

    private HttpResponse<String> fetchOneLeague(int leagueId, int season, String rapidHost, String rapidKey) throws Exception
    {
        URI uri = URI.create("https://" + rapidHost + "/v3/fixtures?league=" + leagueId + "&season=" + season);
        HttpRequest req = HttpRequest.newBuilder(uri)
                .header("X-RapidAPI-Host", rapidHost)
                .header("X-RapidAPI-Key", rapidKey)
                .header("Accept", "application/json")
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static Integer getIntOrNull(JsonNode node, String field)
    {
        JsonNode n = node.get(field);
        return (n != null && n.isInt()) ? n.asInt() : null;
    }

    private static String nullIfBlank(String s)
    {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static Instant parseIsoInstant(String s)
    {
        if (s == null || s.isBlank())
        {
            return null;
        }
        try
        {
            return Instant.parse(s);
        } catch (Exception e)
        {
            return null;
        }
    }
}
