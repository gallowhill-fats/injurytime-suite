/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.impl.db;

import com.injurytime.analysis.api.DossierService;
import com.injurytime.analysis.api.DossierService.*;
import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import org.openide.util.Lookup;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@org.openide.util.lookup.ServiceProvider(service = com.injurytime.analysis.api.DossierService.class, position = 100)
public final class DossierServiceDb implements DossierService {

    private final JpaAccess jpa = Lookup.getDefault().lookup(JpaAccess.class);

    @Override
    public Map<Integer, FixtureDossier> loadDossiers(List<Integer> fixtureIds)
    {
        if (jpa == null || fixtureIds == null || fixtureIds.isEmpty())
        {
            return Map.of();
        }
        return jpa.tx(em ->
        {
            Map<Integer, FixtureDossier> out = new LinkedHashMap<>();
            for (Integer fid : fixtureIds)
            {
                var head = loadFixtureHeader(em, fid);
                if (head == null)
                {
                    continue;
                }

                int leagueId = (int) head.get("league_id");
                int season = (int) head.get("season");
                int homeId = (int) head.get("home_team_id");
                int awayId = (int) head.get("away_team_id");
                String home = (String) head.get("home_team");
                String away = (String) head.get("away_team");
                Instant kick = toInstant(head.get("match_date_utc"));

                List<StatRow> statsH = loadTeamStats(em, leagueId, season, homeId);
                List<StatRow> statsA = loadTeamStats(em, leagueId, season, awayId);
                List<WeekPoint> l5H = loadLast5(em, leagueId, season, homeId);
                List<WeekPoint> l5A = loadLast5(em, leagueId, season, awayId);
                List<ScoreBin> hHome = loadHistogramForTeam(em, leagueId, season, homeId, true);
                List<ScoreBin> hAway = loadHistogramForTeam(em, leagueId, season, awayId, false);
                List<ScoreBin> hLg = loadHistogramLeague(em, leagueId, season);
                List<MatchNote> nH = loadLast5Notes(em, leagueId, season, homeId);
                List<MatchNote> nA = loadLast5Notes(em, leagueId, season, awayId);
                List<Unavailable> uH = loadUnavailability(em, homeId);
                List<Unavailable> uA = loadUnavailability(em, awayId);
                List<Scorer> sH = loadTopScorers(em, leagueId, season, homeId);
                List<Scorer> sA = loadTopScorers(em, leagueId, season, awayId);

                var homeAtHome = teamBinsAtHome(em, leagueId, season, homeId);
                var homeAway = teamBinsAway(em, leagueId, season, homeId);
                var awayAtHome = teamBinsAtHome(em, leagueId, season, awayId);
                var awayAway = teamBinsAway(em, leagueId, season, awayId);
                var lgHome = leagueBinsHome(em, leagueId, season);
                var lgAway = leagueBinsAway(em, leagueId, season);

                out.put(fid, new DossierService.FixtureDossier(
                        fid.intValue(), leagueId, season, homeId, awayId, home, away, kick,
                        statsH, statsA, l5H, l5A,
                        hHome, hAway, hLg,
                        homeAtHome, homeAway, awayAtHome, awayAway, lgHome, lgAway,
                        nH, nA, uH, uA, sH, sA
                ));
            }
            return out;
        });
    }

    // --- header (fixture + teams) ---
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadFixtureHeader(EntityManager em, int fixtureId)
    {
        var row = em.createNativeQuery("""
      SELECT f.league_id, f.season, f.match_date_utc,
             ft.home_team_id, ft.away_team_id, ft.home_team, ft.away_team
        FROM fixture f
        JOIN fixture_teams ft USING (fixture_id)
       WHERE f.fixture_id = :fid
    """).setParameter("fid", fixtureId).getSingleResult();
        if (row == null)
        {
            return null;
        }
        Object[] r = (Object[]) row;
        Map<String, Object> m = new HashMap<>();
        m.put("league_id", ((Number) r[0]).intValue());
        m.put("season", ((Number) r[1]).intValue());
        m.put("match_date_utc", r[2]);
        m.put("home_team_id", ((Number) r[3]).intValue());
        m.put("away_team_id", ((Number) r[4]).intValue());
        m.put("home_team", (String) r[5]);
        m.put("away_team", (String) r[6]);
        return m;
    }

    // --- team_stats() helper (created in your V019 migration) ---
    @SuppressWarnings("unchecked")
    private List<StatRow> loadTeamStats(EntityManager em, int leagueId, int season, int teamId)
    {
        var rows = em.createNativeQuery("""
      SELECT stat, cnt, pct, per_game
        FROM team_stats(:l,:s,:t)
      ORDER BY stat
    """).setParameter("l", leagueId)
                .setParameter("s", season)
                .setParameter("t", teamId)
                .getResultList();

        List<StatRow> out = new ArrayList<>();
        for (Object o : rows)
        {
            Object[] r = (Object[]) o;
            out.add(new StatRow(
                    (String) r[0],
                    (r[1] == null ? null : ((Number) r[1]).intValue()),
                    (r[2] == null ? null : ((Number) r[2]).doubleValue()),
                    (r[3] == null ? null : ((Number) r[3]).doubleValue())
            ));
        }
        return out;
    }

    // --- last 5 as week points, 3-1-0 ---
    @SuppressWarnings("unchecked")
    private List<WeekPoint> loadLast5(EntityManager em, int leagueId, int season, int teamId)
    {
        var rows = em.createNativeQuery("""
      SELECT COALESCE(fixture_week_no(f.round_name),0) AS wk,
             CASE
               WHEN r.res='W' THEN 3
               WHEN r.res='D' THEN 1
               ELSE 0
             END AS pts
      FROM v_team_match_row r
      JOIN fixture f ON f.fixture_id=r.fixture_id
      WHERE r.league_id=:l AND r.season=:s AND r.team_api_id=:t
        AND is_finished(r.status_short)
      ORDER BY r.match_date_utc DESC
      LIMIT 5
    """).setParameter("l", leagueId)
                .setParameter("s", season)
                .setParameter("t", teamId)
                .getResultList();

        List<WeekPoint> out = new ArrayList<>();
        for (Object o : rows)
        {
            Object[] r = (Object[]) o;
            out.add(new WeekPoint(((Number) r[0]).intValue(), ((Number) r[1]).doubleValue()));
        }
        // show oldest → newest in chart
        Collections.reverse(out);
        return out;
    }

    // --- histograms (home / away / league) --
    @SuppressWarnings("unchecked")
    private List<ScoreBin> loadHistogramForTeam(EntityManager em, int leagueId, int season,
            Integer teamId, boolean isHome)
    {
        String sql = """
    SELECT s.bin, COUNT(*)::int AS cnt
    FROM (
      SELECT CASE
               WHEN ft.ft_home_goals <= 7 AND ft.ft_away_goals <= 7
                 THEN (ft.ft_home_goals::text || '-' || ft.ft_away_goals::text)
               ELSE 'other'
             END AS bin
      FROM fixture f
      JOIN fixture_teams ft ON ft.fixture_id = f.fixture_id
      WHERE f.league_id = :lid
        AND f.season    = :season
        AND is_finished(f.status_short)
        AND (
              :teamId IS NULL
              OR (:isHome = TRUE  AND ft.home_team_id = :teamId)
              OR (:isHome = FALSE AND ft.away_team_id = :teamId)
            )
    ) s
    GROUP BY s.bin
    ORDER BY
      (s.bin = 'other') ASC,
      CASE WHEN s.bin <> 'other' THEN split_part(s.bin,'-',1)::int END NULLS LAST,
      CASE WHEN s.bin <> 'other' THEN split_part(s.bin,'-',2)::int END NULLS LAST
  """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("lid", leagueId)
                .setParameter("season", season)
                .setParameter("teamId", teamId) // may be null
                .setParameter("isHome", Boolean.valueOf(isHome)) // Boolean
                .getResultList();

        return toBins(rows);
    }

    @SuppressWarnings("unchecked")
    private List<ScoreBin> loadHistogramLeague(EntityManager em, int leagueId, int season)
    {
        String sql = """
    SELECT s.bin, COUNT(*)::int AS cnt
    FROM (
      SELECT CASE
               WHEN ft.ft_home_goals <= 7 AND ft.ft_away_goals <= 7
                 THEN (ft.ft_home_goals::text || '-' || ft.ft_away_goals::text)
               ELSE 'other'
             END AS bin
      FROM fixture f
      JOIN fixture_teams ft ON ft.fixture_id = f.fixture_id
      WHERE f.league_id = :lid
        AND f.season    = :season
        AND is_finished(f.status_short)
    ) s
    GROUP BY s.bin
    ORDER BY
      (s.bin = 'other') ASC,
      CASE WHEN s.bin <> 'other' THEN split_part(s.bin,'-',1)::int END NULLS LAST,
      CASE WHEN s.bin <> 'other' THEN split_part(s.bin,'-',2)::int END NULLS LAST
  """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("lid", leagueId)
                .setParameter("season", season)
                .getResultList();

        return toBins(rows);
    }

    ///** Common row->DTO mapper for both queries. */
//private static List<ScoreBin> toBins(List<Object[]> rows) {
//  List<ScoreBin> out = new java.util.ArrayList<>(rows.size());
//  for (Object[] r : rows) {
//    String label = (r[0] == null ? "other" : r[0].toString());
//    int count    = ((Number) r[1]).intValue();
//    out.add(new ScoreBin(label, count));
//  }
//  return out;
//}



  // --- last-5 summaries (scorers + reds) ---
  @SuppressWarnings("unchecked")
    private List<MatchNote> loadLast5Notes(EntityManager em, int leagueId, int season, int teamId)
    {
        var fixtures = em.createNativeQuery("""
      SELECT r.fixture_id, r.is_home
      FROM v_team_match_row r
      WHERE r.league_id = :l AND r.season = :s AND r.team_api_id = :t
        AND is_finished(r.status_short)
      ORDER BY r.match_date_utc DESC
      LIMIT 5
    """)
                .setParameter("l", leagueId)
                .setParameter("s", season)
                .setParameter("t", teamId)
                .getResultList();

        List<MatchNote> out = new ArrayList<>();
        for (Object row : fixtures)
        {
            Object[] r = (Object[]) row;
            int fid = ((Number) r[0]).intValue();

            // Event mapping: ev_type/ev_detail -> ev_kind, plus player & time
            var evs = em.createNativeQuery("""
      SELECT
        CASE
          WHEN lower(e.ev_type) = 'goal' AND lower(e.ev_detail) = 'penalty'   THEN 'penalty'
          WHEN lower(e.ev_type) = 'goal' AND lower(e.ev_detail) = 'own goal'  THEN 'own-goal'
          WHEN lower(e.ev_type) = 'goal'                                      THEN 'goal'
          WHEN lower(e.ev_type) = 'card' AND lower(e.ev_detail) LIKE '%red%'  THEN 'red-card'
          ELSE 'other'
        END                       AS ev_kind,
        e.player_name::text       AS player_name,
        e.minute::int            AS minute
      FROM fixture_event e
      WHERE e.fixture_id = :fid
      ORDER BY e.minute NULLS LAST, e.event_id
    """).setParameter("fid", fid)
                    .getResultList();

            if (evs.isEmpty())
            {
                out.add(new MatchNote("Fixture " + fid + ": (no detailed events)"));
                continue;
            }

            String line = ((List<Object[]>) evs).stream()
                    .map(a ->
                    {
                        String type = (String) a[0];
                        String p = (String) a[1];
                        Integer m = (a[2] == null ? null : ((Number) a[2]).intValue());
                        String mt = (m == null ? "" : m + "'");
                        return switch (type == null ? "" : type.toLowerCase())
                        {
                            case "goal" ->
                                p + " " + mt;
                            case "penalty" ->
                                p + " (P) " + mt;
                            case "own-goal" ->
                                p + " (OG) " + mt;
                            case "red-card" ->
                                p + " [RED] " + mt;
                            default ->
                                null; // drop non-target events
                        };
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));

            out.add(new MatchNote("Fixture " + fid + ": " + line));
        }
        Collections.reverse(out); // oldest → newest
        return out;
    }

    // --- current_unavailability(p_team) helper you created earlier ---
    @SuppressWarnings("unchecked")
    private List<Unavailable> loadUnavailability(EntityManager em, int teamId)
    {
        var rows = em.createNativeQuery("""
      SELECT player_api_id, player_name, availability_type, status, notes
      FROM current_unavailability(:t)
    """).setParameter("t", teamId).getResultList();

        List<Unavailable> out = new ArrayList<>();
        for (Object o : rows)
        {
            Object[] r = (Object[]) o;
            out.add(new Unavailable(
                    ((Number) r[0]).intValue(),
                    (String) r[1],
                    (String) r[2],
                    (String) r[3],
                    (String) r[4]
            ));
        }
        return out;
    }

    // --- top scorers this season for club (count goals in events) ---
    @SuppressWarnings("unchecked")
    private List<Scorer> loadTopScorers(EntityManager em, int leagueId, int season, int teamId)
    {
        var rows = em.createNativeQuery("""
      SELECT coalesce(e.player_api_id, 0) AS pid,
             coalesce(e.player_name, 'Unknown') AS pname,
             COUNT(*)::int AS g
      FROM fixture f
      JOIN fixture_event e ON e.fixture_id=f.fixture_id
      JOIN fixture_teams ft ON ft.fixture_id=f.fixture_id
      WHERE f.league_id=:l AND f.season=:s AND is_finished(f.status_short)
        AND lower(coalesce(e.ev_type,'')) IN ('goal','penalty')  -- exclude OG
        AND (ft.home_team_id=:t OR ft.away_team_id=:t)
      GROUP BY pid, pname
      ORDER BY g DESC, pname
      LIMIT 10
    """).setParameter("l", leagueId)
                .setParameter("s", season)
                .setParameter("t", teamId)
                .getResultList();

        List<Scorer> out = new ArrayList<>();
        for (Object o : rows)
        {
            Object[] r = (Object[]) o;
            out.add(new Scorer(((Number) r[0]).intValue(), (String) r[1], ((Number) r[2]).intValue()));
        }
        return out;
    }

    private static Instant toInstant(Object o)
    {
        if (o == null)
        {
            return null;
        }
        if (o instanceof Timestamp ts)
        {
            return ts.toInstant();
        }
        if (o instanceof java.util.Date d)
        {
            return d.toInstant();
        }
        if (o instanceof Instant i)
        {
            return i;
        }
        return null;
    }

    // Accepts rows in one of these shapes:
//   1) [gf(Number), ga(Number), n(Number)]
//   2) [label(CharSequence "a-b"), n(Number)]
//   3) [label(CharSequence "a-b"), <ignored>, n(Number)]  // some drivers add extra cols
    @SuppressWarnings("unchecked")
    private static java.util.List<DossierService.ScoreBin> toBins(java.util.List<?> rows)
    {
        var out = new java.util.ArrayList<DossierService.ScoreBin>();

        for (Object row : rows)
        {
            Object[] a = (Object[]) row;

            // Case A: first column looks like "a-b"
            if (a.length >= 2 && isLabel(a[0]))
            {
                String label = a[0].toString().trim();
                Object countCol = (a.length == 2 ? a[1] : a[a.length - 1]); // take last as count
                int n = asInt(countCol);
                out.add(new DossierService.ScoreBin(label, n));
                continue;
            }

            // Case B: numeric gf,ga,(n) in first 3 columns
            if (a.length >= 3)
            {
                int gf = asInt(a[0]);
                int ga = asInt(a[1]);
                int n = asInt(a[2]);
                out.add(new DossierService.ScoreBin(gf + "-" + ga, n));
                continue;
            }

            // Fallback: try to interpret 2 columns as [gf,ga] with implicit n=1
            if (a.length == 2)
            {
                int gf = asInt(a[0]);
                int ga = asInt(a[1]);
                out.add(new DossierService.ScoreBin(gf + "-" + ga, 1));
                continue;
            }

            // If we get here, row shape is unexpected — skip silently or log once
            // System.out.println("toBins: unexpected row shape len=" + a.length + " -> " + java.util.Arrays.toString(a));
        }

        // sort by (gf, ga), "other" last if present
        out.sort(java.util.Comparator
                .comparingInt((DossierService.ScoreBin b) ->
                {
                    String lab = b.label();
                    if ("other".equalsIgnoreCase(lab))
                    {
                        return Integer.MAX_VALUE;
                    }
                    int i = lab.indexOf('–');
                    if (i < 0)
                    {
                        i = lab.indexOf('-');
                    }
                    return (i > 0) ? safeParseInt(lab.substring(0, i).trim(), Integer.MAX_VALUE) : Integer.MAX_VALUE;
                })
                .thenComparingInt(b ->
                {
                    String lab = b.label();
                    if ("other".equalsIgnoreCase(lab))
                    {
                        return Integer.MAX_VALUE;
                    }
                    int i = lab.indexOf('–');
                    if (i < 0)
                    {
                        i = lab.indexOf('-');
                    }
                    return (i > 0) ? safeParseInt(lab.substring(i + 1).trim(), Integer.MAX_VALUE) : Integer.MAX_VALUE;
                })
        );
        return out;
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<DossierService.ScoreBin> toBins3(java.util.List<?> rows)
    {
        var out = new java.util.ArrayList<DossierService.ScoreBin>(rows.size());
        for (Object r : rows)
        {
            Object[] a = (Object[]) r;                 // [gf, ga, n]
            int gf = ((Number) a[0]).intValue();
            int ga = ((Number) a[1]).intValue();
            int n = ((Number) a[2]).intValue();
            out.add(new DossierService.ScoreBin(gf + "-" + ga, n));
        }
        out.sort(java.util.Comparator
                .comparingInt((DossierService.ScoreBin b) -> Integer.parseInt(b.label().split("[-–]")[0]))
                .thenComparingInt(b -> Integer.parseInt(b.label().split("[-–]")[1])));
        return out;
    }

    private static boolean isLabel(Object o)
    {
        if (!(o instanceof CharSequence s))
        {
            return false;
        }
        String t = s.toString();
        int i = t.indexOf('–');
        if (i < 0)
        {
            i = t.indexOf('-');
        }
        if (i <= 0 || i >= t.length() - 1)
        {
            return false;
        }
        // quick numeric check around the dash
        try
        {
            Integer.parseInt(t.substring(0, i).trim());
            Integer.parseInt(t.substring(i + 1).trim());
            return true;
        } catch (NumberFormatException e)
        {
            return false;
        }
    }

    private static int safeParseInt(String s, int def)
    {
        try
        {
            return Integer.parseInt(s);
        } catch (Exception e)
        {
            return def;
        }
    }

    private List<DossierService.ScoreBin> teamBinsAtHome(EntityManager em, int leagueId, int season, int teamId)
    {
        var rows = em.createNativeQuery("""
    SELECT
      ft.ft_home_goals AS gf,
      ft.ft_away_goals AS ga,
      COUNT(*)::int AS n
    FROM fixture_teams ft
    JOIN fixture f ON f.fixture_id = ft.fixture_id
    WHERE f.league_id = :l AND f.season = :s
      AND ft.home_team_id = :t
      AND (f.status_long = 'Match finished' OR f.status_short = 'FT')
    GROUP BY gf, ga
    ORDER BY gf, ga
  """)
                .setParameter("l", leagueId).setParameter("s", season).setParameter("t", teamId)
                .getResultList();
        return toBins3(rows);
    }

    private List<DossierService.ScoreBin> teamBinsAway(EntityManager em, int leagueId, int season, int teamId)
    {
        var rows = em.createNativeQuery("""
    SELECT
      ft.ft_away_goals AS gf,
      ft.ft_home_goals AS ga,
      COUNT(*)::int AS n
    FROM fixture_teams ft
    JOIN fixture f ON f.fixture_id = ft.fixture_id
    WHERE f.league_id = :l AND f.season = :s
      AND ft.away_team_id = :t
      AND (f.status_long = 'Match finished' OR f.status_short = 'FT')
    GROUP BY gf, ga
    ORDER BY gf, ga
  """)
                .setParameter("l", leagueId).setParameter("s", season).setParameter("t", teamId)
                .getResultList();
        return toBins3(rows);
    }

    private List<DossierService.ScoreBin> leagueBinsHome(EntityManager em, int leagueId, int season)
    {
        var rows = em.createNativeQuery("""
    SELECT
      ft.ft_home_goals AS gf,
      ft.ft_away_goals AS ga,
      COUNT(*)::int AS n
    FROM fixture_teams ft
    JOIN fixture f ON f.fixture_id = ft.fixture_id
    WHERE f.league_id = :l AND f.season = :s
      AND (f.status_long = 'Match finished' OR f.status_short = 'FT')
    GROUP BY gf, ga
    ORDER BY gf, ga
  """)
                .setParameter("l", leagueId).setParameter("s", season)
                .getResultList();
        return toBins3(rows);
    }

    private List<DossierService.ScoreBin> leagueBinsAway(EntityManager em, int leagueId, int season)
    {
        var rows = em.createNativeQuery("""
    SELECT
      ft.ft_away_goals AS gf,
      ft.ft_home_goals AS ga,
      COUNT(*)::int AS n
    FROM fixture_teams ft
    JOIN fixture f ON f.fixture_id = ft.fixture_id
    WHERE f.league_id = :l AND f.season = :s
      AND (f.status_long = 'Match finished' OR f.status_short = 'FT')
    GROUP BY gf, ga
    ORDER BY gf, ga
  """)
                .setParameter("l", leagueId).setParameter("s", season)
                .getResultList();
        return toBins3(rows);
    }

    private static int asInt(Object o)
    {
        if (o == null)
        {
            return 0;
        }
        if (o instanceof Number n)
        {
            return n.intValue();
        }
        if (o instanceof CharSequence s)
        {
            String t = s.toString().trim();
            if (t.isEmpty())
            {
                return 0;
            }
            // handle possible decimal COUNTs like "12.0"
            int dot = t.indexOf('.');
            if (dot > 0)
            {
                t = t.substring(0, dot);
            }
            return Integer.parseInt(t);
        }
        // last resort (helps with JDBC drivers returning e.g. PGobject)
        String t = o.toString().trim();
        if (t.isEmpty())
        {
            return 0;
        }
        int dot = t.indexOf('.');
        if (dot > 0)
        {
            t = t.substring(0, dot);
        }
        return Integer.parseInt(t);
    }

}
