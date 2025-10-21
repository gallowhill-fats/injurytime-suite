/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.pg.fixtures.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class FixturesJsonImporterPg {
  private final JpaAccess jpa;
  private final ObjectMapper mapper = new ObjectMapper();
  
  public record Result(int inserted, int updated, int total) {}

  public FixturesJsonImporterPg(JpaAccess jpa) { this.jpa = jpa; }

  public Result importJson(String json) throws Exception {
    JsonNode root = mapper.readTree(json);
    JsonNode arr = root.path("response");
    if (!arr.isArray() || arr.isEmpty()) return new Result(0,0,0);

    final int[] ins = {0}, upd = {0}, tot = {0};

    jpa.tx(em -> {
      for (JsonNode r : arr) {
        tot[0]++;

        long fixtureId = r.path("fixture").path("id").asLong();
        int leagueId   = r.path("league").path("id").asInt();
        int season     = r.path("league").path("season").asInt();
        String round   = textOrNull(r, "league", "round");
        String dateIso = textOrNull(r, "fixture", "date");
        Timestamp when = dateIso != null ? Timestamp.from(OffsetDateTime.parse(dateIso).toInstant()) : null;

        Integer venueId   = r.path("fixture").path("venue").path("id").isInt() ? r.path("fixture").path("venue").path("id").asInt() : null;
        String  venueName = textOrNull(r, "fixture", "venue", "name");
        String  venueCity = textOrNull(r, "fixture", "venue", "city");
        String  referee   = textOrNull(r, "fixture", "referee");

        String stShort = textOrNull(r, "fixture", "status", "short");
        String stLong  = textOrNull(r, "fixture", "status", "long");
        Integer elapsed = r.path("fixture").path("status").path("elapsed").isInt() ? r.path("fixture").path("status").path("elapsed").asInt() : null;

        int homeId  = r.path("teams").path("home").path("id").asInt();
        String home = textOrNull(r, "teams", "home", "name");
        int awayId  = r.path("teams").path("away").path("id").asInt();
        String away = textOrNull(r, "teams", "away", "name");

        Integer gH  = intOrNull(r, "goals", "home");
        Integer gA  = intOrNull(r, "goals", "away");
        Integer htH = intOrNull(r, "score", "halftime", "home");
        Integer htA = intOrNull(r, "score", "halftime", "away");
        Integer ftH = intOrNull(r, "score", "fulltime", "home");
        Integer ftA = intOrNull(r, "score", "fulltime", "away");

        // --- fixture upsert with "was inserted?" return ---
        Boolean inserted = (Boolean) em.createNativeQuery("""
          WITH upsert AS (
            INSERT INTO fixture(
              fixture_id, league_id, season, round_name, match_date_utc,
              venue_id, venue_name, venue_city, referee,
              status_short, status_long, elapsed_minutes
            ) VALUES (
              :fid, :lid, :season, :round, :when,
              :vid, :vname, :vcity, :ref,
              :s_short, :s_long, :elapsed
            )
            ON CONFLICT (fixture_id) DO UPDATE SET
              league_id=EXCLUDED.league_id,
              season=EXCLUDED.season,
              round_name=EXCLUDED.round_name,
              match_date_utc=EXCLUDED.match_date_utc,
              venue_id=EXCLUDED.venue_id,
              venue_name=EXCLUDED.venue_name,
              venue_city=EXCLUDED.venue_city,
              referee=EXCLUDED.referee,
              status_short=EXCLUDED.status_short,
              status_long=EXCLUDED.status_long,
              elapsed_minutes=EXCLUDED.elapsed_minutes
            RETURNING (xmax = 0) AS inserted
          )
          SELECT coalesce(bool_or(inserted), false) FROM upsert
        """)
          .setParameter("fid", fixtureId)
          .setParameter("lid", leagueId)
          .setParameter("season", season)
          .setParameter("round", round)
          .setParameter("when", when)
          .setParameter("vid", venueId)
          .setParameter("vname", venueName)
          .setParameter("vcity", venueCity)
          .setParameter("ref", referee)
          .setParameter("s_short", stShort)
          .setParameter("s_long", stLong)
          .setParameter("elapsed", elapsed)
          .getSingleResult();

        if (Boolean.TRUE.equals(inserted)) ins[0]++; else upd[0]++;

        // --- fixture_teams upsert (no need to count separately) ---
        em.createNativeQuery("""
          INSERT INTO fixture_teams(
            fixture_id, home_team_id, home_team, away_team_id, away_team,
            home_goals, away_goals, ht_home_goals, ht_away_goals, ft_home_goals, ft_away_goals
          ) VALUES (
            :fid, :hid, :hname, :aid, :aname,
            :gh, :ga, :hth, :hta, :fth, :fta
          )
          ON CONFLICT (fixture_id) DO UPDATE SET
            home_team_id=EXCLUDED.home_team_id,
            home_team=EXCLUDED.home_team,
            away_team_id=EXCLUDED.away_team_id,
            away_team=EXCLUDED.away_team,
            home_goals=EXCLUDED.home_goals,
            away_goals=EXCLUDED.away_goals,
            ht_home_goals=EXCLUDED.ht_home_goals,
            ht_away_goals=EXCLUDED.ht_away_goals,
            ft_home_goals=EXCLUDED.ft_home_goals,
            ft_away_goals=EXCLUDED.ft_away_goals
        """)
          .setParameter("fid", fixtureId)
          .setParameter("hid", homeId)
          .setParameter("hname", home)
          .setParameter("aid", awayId)
          .setParameter("aname", away)
          .setParameter("gh", gH)
          .setParameter("ga", gA)
          .setParameter("hth", htH)
          .setParameter("hta", htA)
          .setParameter("fth", ftH)
          .setParameter("fta", ftA)
          .executeUpdate();
      }
      return null;
    });

    return new Result(ins[0], upd[0], tot[0]);
  }

  private static String textOrNull(JsonNode root, String... path) {
    JsonNode n = root;
    for (String p : path) n = n.path(p);
    return n.isMissingNode() || n.isNull() ? null : n.asText();
  }
  private static Integer intOrNull(JsonNode root, String... path) {
    JsonNode n = root;
    for (String p : path) n = n.path(p);
    return n.isInt() ? n.asInt() : null;
  }
}

