/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.pg.tools.squad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.injurytime.storage.api.AppConfigExt;
import com.injurytime.storage.api.JpaAccess; // your interface
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import java.util.logging.Logger;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileChooserBuilder;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Tools", id = "com.injurytime.pg.tools.squads.BulkUpdateSquadsAction")
@ActionRegistration(displayName = "#CTL_BulkUpdateSquads")
@ActionReference(path = "Menu/Tools", position = 1830)
@Messages("CTL_BulkUpdateSquads=Squads: Bulk Update from API (by club list)")
public final class BulkUpdateSquadsAction implements ActionListener {

    // ---- API config
    private static final String RAPIDAPI_HOST = "api-football-v1.p.rapidapi.com";
    private static final String SQUADS_URL = "https://api-football-v1.p.rapidapi.com/v3/players/squads";
    private static final String CONTENT_TYPE = "application/json;charset=UTF-8";

    // Read your key from env / system property / properties file via your existing AppConfig
    private static String rapidKey()
    {
        // AppConfig.get("ApiFootball.token") if you wired it; fallback to env
        String v = com.injurytime.storage.api.AppConfig.get("RAPID_KEY"); // adjust package if needed
        if (v == null || v.isBlank())
        {
            v = System.getenv("APIFOOTBALL_TOKEN");
        }
        return v;
    }

    // ---- Rate-limit guard thresholds
    private static final int WARN_REMAINING = 100;  // warn when ≤ this
    private static final int HARD_STOP = 5;    // stop when ≤ this

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    record ClubRow(int apiClubId, String abbr) {

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        int season = AppConfigExt.getIntOrNull("injurytime.season.current");
        File start = new File(System.getProperty("user.home"));
        File file = new FileChooserBuilder("squads-bulk-updater")
                .setTitle("Select club list (api_club_id,club_abbr)")
                .setDefaultWorkingDirectory(start)
                .setFilesOnly(true)
                .showOpenDialog();
        if (file == null)
        {
            return;
        }

        final String key = rapidKey();
        if (key == null || key.isBlank())
        {
            org.openide.DialogDisplayer.getDefault().notify(
                    new org.openide.NotifyDescriptor.Message(
                            "API key not found. Set ApiFootball.token (AppConfig) or APIFOOTBALL_TOKEN env.",
                            org.openide.NotifyDescriptor.ERROR_MESSAGE));
            return;
        }

        BaseProgressUtils.showProgressDialogAndRun(() ->
        {
            try
            {
                var rows = readClubList(file);
                if (rows.isEmpty())
                {
                    info("No club rows found.");
                    return;
                }
                var jpa = Lookup.getDefault().lookup(JpaAccess.class);
                if (jpa == null)
                {
                    info("No JpaAccess service available.");
                    return;
                }

                int scanned = 0, calls = 0, clubUpdated = 0, playersUp = 0, rosterUp = 0, errs = 0;
                Integer lastLimit = null, lastRemaining = null;
                boolean warned = false;

                for (ClubRow row : rows)
                {
                    scanned++;
                    try
                    {
                        // ---- HTTP call
                        URI uri = URI.create(SQUADS_URL + "?team=" + row.apiClubId());
                        HttpRequest req = HttpRequest.newBuilder(uri)
                                .header("X-RapidAPI-Host", RAPIDAPI_HOST)
                                .header("X-RapidAPI-Key", key)
                                .header("Accept", "application/json")
                                .header("Content-Type", CONTENT_TYPE)
                                .GET()
                                .build();

                        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                        calls++;

                        // ---- rate headers
                        lastLimit = headerInt(resp, "x-ratelimit-requests-limit", lastLimit);
                        lastRemaining = headerInt(resp, "x-ratelimit-requests-remaining", lastRemaining);

                        if (resp.statusCode() == 429)
                        {
                            errs++;
                            info("HTTP 429 Too Many Requests. Stopping early.");
                            break;
                        }
                        if (resp.statusCode() / 100 != 2)
                        {
                            errs++;
                            // gentle backoff
                            Thread.sleep(200);
                            continue;
                        }
                        if (lastRemaining != null)
                        {
                            if (!warned && lastRemaining <= WARN_REMAINING)
                            {
                                System.out.println("[Squads] Warning: only " + lastRemaining + " requests remaining"
                                        + (lastLimit != null ? (" out of " + lastLimit) : ""));
                                warned = true;
                            }
                            if (lastRemaining <= HARD_STOP)
                            {
                                System.out.println("[Squads] Hard stop at remaining=" + lastRemaining);
                                break;
                            }
                        }

                        // ---- parse response
                        JsonNode root = mapper.readTree(resp.body());
                        JsonNode response = root.path("response");
                        if (!response.isArray() || response.size() == 0)
                        {
                            // nothing for this club
                            continue;
                        }
                        JsonNode block = response.get(0);
                        // team info
                        int apiClubId = block.path("team").path("id").asInt(row.apiClubId());
                        String clubName = nullIfBlank(block.path("team").path("name").asText(null));
                        String clubLogo = nullIfBlank(block.path("team").path("logo").asText(null));

                        // -- Players array
                        JsonNode players = block.path("players");
                        if (!players.isArray())
                        {
                            players = mapper.createArrayNode();
                        }

// Make an effectively-final copy to iterate inside the lambda
                        final java.util.List<JsonNode> playerNodes = new java.util.ArrayList<>();
                        players.forEach(playerNodes::add);

// ---- one transaction per club
                        final int[] locPlayers =
                        {
                            0
                        }, locRoster =
                        {
                            0
                        }, locClub =
                        {
                            0
                        };
                        jpa.tx((EntityManager em) ->
                        {
                            // ensure club row and update logo/name
//                            ensureClubExistsAndUpdate(em, apiClubId, clubName, clubLogo);
//                            locClub[0] = 1;
                            int logoUpdated = em.createNativeQuery("""
                            UPDATE club
                            SET logo_url = COALESCE(:logo, logo_url),
                            updated_at = now()
                            WHERE api_club_id = :cid
                           """)
                                    .setParameter("logo", clubLogo /* or clubLogo if this is the club logo source */)
                                    .setParameter("cid", apiClubId)
                                    .executeUpdate();

                            if (logoUpdated == 0)
                            {
                                // club not present — do not create it here; just log and skip
                                // (we don't want to touch club table from the squad importer)
                                Logger.getAnonymousLogger().info("Club not found; seed clubs first: " + apiClubId);
                            }

                            for (JsonNode p : playerNodes)
                            {
                                Integer playerId = getIntOrNull(p, "id");
                                String playerName = nullIfBlank(p.path("name").asText(null));
                                Integer shirtNo = getIntOrNull(p, "number");
                                String position = nullIfBlank(p.path("position").asText(null));
                                String photoUrl = nullIfBlank(p.path("photo").asText(null));

                                if (playerId == null || playerName == null)
                                {
                                    continue;
                                }

                                // upsert player
                                em.createNativeQuery("""
        SELECT upsert_player(:pid, :pname, NULL, NULL, NULL, :img)
      """)
                                        .setParameter("pid", playerId)
                                        .setParameter("pname", playerName)
                                        .setParameter("img", photoUrl)
                                        .getSingleResult();
                                locPlayers[0]++;

                                // ensure roster tuple
                                em.createNativeQuery("""
        SELECT ensure_squad_roster(
          :season, :cid, :pid, :pos, :shirt, NULL, NULL, NULL, NULL
        )
      """)
                                        .setParameter("season", String.valueOf(season))
                                        .setParameter("cid", apiClubId)
                                        .setParameter("pid", playerId)
                                        .setParameter("pos", position)
                                        .setParameter("shirt", shirtNo)
                                        .getSingleResult();
                                locRoster[0]++;
                            }
                            return null;
                        });

                        clubUpdated += locClub[0];
                        playersUp += locPlayers[0];
                        rosterUp += locRoster[0];

                        // gentle pacing
                        Thread.sleep(100);

                    } catch (Exception ex)
                    {
                        errs++;
                    }
                }

                info("Done. Clubs: " + scanned + ", API calls: " + calls
                        + ", clubs touched: " + clubUpdated
                        + ", players upserted: " + playersUp
                        + ", roster rows ensured: " + rosterUp
                        + ", errors: " + errs
                        + (lastLimit != null ? (", daily limit: " + lastLimit) : "")
                        + (lastRemaining != null ? (", remaining: " + lastRemaining) : "")
                );

            } catch (Exception ex)
            {
                info("Bulk squads update failed: " + ex.getMessage());
            }
        }, "Fetching & updating squads...");
    }

    // -------- helpers
    private static List<ClubRow> readClubList(File f) throws IOException
    {
        try (BufferedReader br = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8))
        {
            return br.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                    .skip(isHeader(br, f) ? 1 : 0) // if you want to auto-skip header, otherwise remove this
                    .map(line ->
                    {
                        // accept CSV "api_club_id,abbr" or just "api_club_id"
                        String[] parts = line.split(",", 2);
                        try
                        {
                            int id = Integer.parseInt(parts[0].trim());
                            String abbr = (parts.length > 1) ? parts[1].trim() : "";
                            return new ClubRow(id, abbr);
                        } catch (Exception ignored)
                        {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    private static boolean isHeader(BufferedReader br, File f)
    {
        // simple: if first line contains non-digits before comma, treat as header
        try
        {
            br.mark(2000);
            String first = br.readLine();
            br.reset();
            if (first == null)
            {
                return false;
            }
            return first.toLowerCase().contains("api_club_id");
        } catch (IOException e)
        {
            return false;
        }
    }

    private static Integer headerInt(HttpResponse<?> resp, String name, Integer prev)
    {
        return resp.headers().firstValue(name)
                .map(v ->
                {
                    try
                    {
                        return Integer.parseInt(v.trim());
                    } catch (Exception ignore)
                    {
                        return prev;
                    }
                })
                .orElse(prev);
    }

    private static Integer getIntOrNull(JsonNode n, String field)
    {
        return n.hasNonNull(field) && n.get(field).canConvertToInt() ? n.get(field).asInt() : null;
    }

    private static String nullIfBlank(String s)
    {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static int currentSeasonGuess()
    {
        // quick helper—replace with your app’s current season logic if available
        java.time.LocalDate now = java.time.LocalDate.now();
        int y = now.getYear();
        return y;
    }

    private static void ensureClubExistsAndUpdate(EntityManager em, int apiClubId, String clubName, String logoUrl)
    {
        // Prefer your SQL helpers (upsert_club) if you like:
        if (clubName != null || logoUrl != null)
        {
            em.createNativeQuery("""
          SELECT upsert_club(:cid, :name, NULL, NULL, :logo)
        """)
                    .setParameter("cid", apiClubId)
                    .setParameter("name", clubName)
                    .setParameter("logo", logoUrl)
                    .getSingleResult();
        } else
        {
            // at least ensure row exists
            em.createNativeQuery("""
          INSERT INTO club(api_club_id, club_name)
          VALUES (:cid, coalesce(:name,'Club '||:cid))
          ON CONFLICT (api_club_id) DO NOTHING
        """)
                    .setParameter("cid", apiClubId)
                    .setParameter("name", clubName)
                    .executeUpdate();
        }
    }

    private static void info(String msg)
    {
        org.openide.DialogDisplayer.getDefault().notify(
                new org.openide.NotifyDescriptor.Message(msg, org.openide.NotifyDescriptor.INFORMATION_MESSAGE));
    }
}
