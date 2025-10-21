/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.pg.fixtures.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.injurytime.storage.api.AppConfig;
import com.injurytime.storage.api.AppConfigExt;
import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

final class EventsFetcher {
   
    record Result(int scanned, int calls, int inserted, int updated, int rawUpserts, int errors,
                Integer dailyLimit, Integer remainingAfter) {}

  private final JpaAccess jpa;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
  
  
  // API-Football settings
String rapidHost = AppConfig.get("RAPID_HOST"); // e.g. api-football-v1.p.rapidapi.com
String rapidKey  = AppConfig.get("RAPID_KEY");  // your secret key

// Rate-limit guards (with small helpers below)
int warnRemaining = AppConfigExt.getInt("RAPID_WARN_REMAINING", 250);
int hardStop      = AppConfigExt.getInt("RAPID_HARD_STOP", 50);

// DB (if needed in a module)
String jdbcUrl  = AppConfig.get("PG_JDBC_URL");
String dbUser   = AppConfig.get("PG_USER");
String dbPass   = AppConfig.get("PG_PASSWORD");

  // Load from injurytime.local.properties (same place you keep DB creds)
  

  EventsFetcher(JpaAccess jpa) {
    this.jpa = jpa;
    
    if (rapidKey == null || rapidKey.isBlank())
      throw new IllegalStateException("Missing apifootball.rapidapi.key in injurytime.local.properties");

    this.warnRemaining = parseIntOrDefault(AppConfig.get("apifootball.ratelimit.warnRemaining"), 250);
    this.hardStop      = parseIntOrDefault(AppConfig.get("apifootball.ratelimit.hardStop"), 25);
  }

  private static int parseIntOrDefault(String s, int def) {
    try { return (s == null) ? def : Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
  }

  Result fetchAndUpsertForSeason(int leagueId, int season) {
    final int[] scanned={0}, calls={0}, ins={0}, upd={0}, rawUp={0}, err={0};
    final Integer[] lastLimit = {null}, lastRemaining = {null};
    final boolean[] warned = {false};

    List<Long> fixtures = jpa.tx((EntityManager em) -> {
      return em.createNativeQuery("""
        SELECT f.fixture_id
        FROM fixture f
        WHERE f.league_id = :lid AND f.season = :season
          AND (
               f.fixture_id NOT IN (SELECT fixture_id FROM fixture_events_raw)
               OR coalesce(f.status_short,'') NOT IN ('FT','AET','PEN')  -- refresh live/in-progress
          )
        ORDER BY f.match_date_utc
      """)
      .setParameter("lid", leagueId)
      .setParameter("season", season)
      .getResultList()
      .stream().map(n -> ((Number)n).longValue()).toList();
    });

    for (Long fid : fixtures) {
      scanned[0]++;

      try {
        // ---- call API
        URI uri = URI.create("https://" + rapidHost + "/v3/fixtures/events?fixture=" + fid);
        HttpRequest req = HttpRequest.newBuilder(uri)
          .header("X-RapidAPI-Host", rapidHost)
          .header("X-RapidAPI-Key", rapidKey)
          .header("Accept", "application/json")
          .GET()
          .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        calls[0]++;

        // ---- capture rate headers
        // x-ratelimit-requests-limit / x-ratelimit-requests-remaining
        resp.headers().firstValue("x-ratelimit-requests-limit").ifPresent(v -> {
          try { lastLimit[0] = Integer.parseInt(v.trim()); } catch (Exception ignored) {}
        });
        resp.headers().firstValue("x-ratelimit-requests-remaining").ifPresent(v -> {
          try { lastRemaining[0] = Integer.parseInt(v.trim()); } catch (Exception ignored) {}
        });

        // ---- react to HTTP status
        int sc = resp.statusCode();
        if (sc == 429) {        // Too Many Requests
          err[0]++;
          // Hard stop immediately; you’ve hit the plan limits for the period
          break;
        }
        if (sc / 100 != 2) {
          err[0]++;
          Thread.sleep(250L);
          // Optionally: if 403/401, you might want to break too
          continue;
        }

        // ---- enforce soft/hard thresholds using headers (if present)
        if (lastRemaining[0] != null) {
          if (!warned[0] && lastRemaining[0] <= warnRemaining) {
            // simple log warning; if you’d like a dialog, you can show it here
            System.out.println("[EventsFetcher] Warning: only " + lastRemaining[0] + " requests remaining out of "
                + (lastLimit[0] != null ? lastLimit[0] : "?"));
            warned[0] = true;
          }
          if (lastRemaining[0] <= hardStop) {
            // stop before chewing through the last few calls
            break;
          }
        }

        // ---- parse + upsert (your existing logic)
        String body = resp.body();
        JsonNode root = mapper.readTree(body);
        JsonNode eventsCandidate = root.path("response");
        final com.fasterxml.jackson.databind.node.ArrayNode EMPTY = mapper.createArrayNode();
        final JsonNode safeEvents = (eventsCandidate != null && eventsCandidate.isArray())
        ? eventsCandidate
        : EMPTY;

        int up = jpa.tx((EntityManager em) -> em.createNativeQuery("""
            INSERT INTO fixture_events_raw (fixture_id, fetched_at, payload)
            VALUES (:fid, now(), CAST(:payload AS jsonb))
            ON CONFLICT (fixture_id) DO UPDATE
              SET fetched_at = EXCLUDED.fetched_at,
                  payload = EXCLUDED.payload
          """)
          .setParameter("fid", fid)
          .setParameter("payload", body)
          .executeUpdate());
        rawUp[0] += (up > 0 ? 1 : 0);

        // clear & reinsert event rows for consistent seq_no
        // ---- upsert event rows for THIS fixture in ONE TX
//int evCount = events.size();

int locIns = jpa.tx(em -> {
  // capture outer-scope vars into final locals for lambda safety
  final long    fixtureId = fid;
  final JsonNode evs      = safeEvents;
  final int      evCount  = (evs != null && evs.isArray()) ? evs.size() : 0;

  // keep seq_no consistent, avoid stale rows
  em.createNativeQuery("DELETE FROM fixture_event WHERE fixture_id = :fid")
    .setParameter("fid", fixtureId)
    .executeUpdate();

  int wrote = 0;

  for (int idx = 0; idx < evCount; idx++) {
    final int     seq = idx + 1;       // effectively final per-iteration
    final JsonNode e  = evs.get(idx);

    final Integer elapsed  = e.path("time").path("elapsed").isInt() ? e.path("time").path("elapsed").asInt() : null;
    final Integer extra    = e.path("time").path("extra").isInt()   ? e.path("time").path("extra").asInt()   : null;

    final Integer teamId   = e.path("team").path("id").isInt()      ? e.path("team").path("id").asInt()      : null;

    final Integer playerId = e.path("player").path("id").isInt()    ? e.path("player").path("id").asInt()    : null;
    final String  playerNm = e.path("player").path("name").isMissingNode() ? null : e.path("player").path("name").asText();

    final Integer assistId = e.path("assist").path("id").isInt()    ? e.path("assist").path("id").asInt()    : null;
    final String  assistNm = e.path("assist").path("name").isMissingNode() ? null : e.path("assist").path("name").asText();

    final String  type     = e.path("type").isMissingNode()         ? null : e.path("type").asText();
    final String  detail   = e.path("detail").isMissingNode()       ? null : e.path("detail").asText();
    final String  comments = e.path("comments").isMissingNode()     ? null : e.path("comments").asText();

    final String  normType = normalizeType(type);

    int changed = em.createNativeQuery("""
        INSERT INTO fixture_event(
          fixture_id, seq_no, minute, minute_extra, team_api_id,
          player_api_id, player_name, assist_api_id, assist_name,
          ev_type, ev_detail, comments
        )
        VALUES (
          :fid, :seq, :min, :xtra, :tid,
          :pid, :pname, :aid, :aname,
          :etype, :edetail, :cmt
        )
        ON CONFLICT (fixture_id, seq_no) DO UPDATE SET
          minute        = EXCLUDED.minute,
          minute_extra  = EXCLUDED.minute_extra,
          team_api_id   = EXCLUDED.team_api_id,
          player_api_id = EXCLUDED.player_api_id,
          player_name   = EXCLUDED.player_name,
          assist_api_id = EXCLUDED.assist_api_id,
          assist_name   = EXCLUDED.assist_name,
          ev_type       = EXCLUDED.ev_type,
          ev_detail     = EXCLUDED.ev_detail,
          comments      = EXCLUDED.comments
      """)
      .setParameter("fid",  fixtureId)
      .setParameter("seq",  seq)
      .setParameter("min",  elapsed)
      .setParameter("xtra", extra)
      .setParameter("tid",  teamId)
      .setParameter("pid",  playerId)
      .setParameter("pname", playerNm)
      .setParameter("aid",  assistId)
      .setParameter("aname", assistNm)
      .setParameter("etype", normType)
      .setParameter("edetail", detail)
      .setParameter("cmt",  comments)
      .executeUpdate();

    if (changed > 0) wrote++;
  }

  return wrote; // autoboxed to Integer for the Function<Em, Integer>
});


        // First time = inserted, subsequent runs overwrite = updated
        if (locIns > 0) {
          if (rawUp[0] == 1) ins[0] += locIns; else upd[0] += locIns;
        }

        // gentle pacing
        Thread.sleep(120L);

      } catch (Exception ex) {
        err[0]++;
      }
    }

    return new Result(scanned[0], calls[0], ins[0], upd[0], rawUp[0], err[0], lastLimit[0], lastRemaining[0]);
  }

  private static String normalizeType(String t) {
    if (t == null) return "unknown";
    String s = t.trim().toLowerCase();
    if (s.equals("goal")) return "goal";
    if (s.equals("card")) return "card";
    if (s.startsWith("subst")) return "subst";
    if (s.contains("pen")) return "penalty";  // handles "Penalty", "Missed Penalty" elsewhere
    if (s.equals("var")) return "var";
    return s;
  }
}

