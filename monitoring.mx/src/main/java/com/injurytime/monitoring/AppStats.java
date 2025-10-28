/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.monitoring;


import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class AppStats implements AppStatsMXBean {
  private final JpaAccess jpa = Lookup.getDefault().lookup(JpaAccess.class);

  // cache state
  private volatile int playersCount, clubsCount, squadsCount;
  private volatile long fixturesCount, eventsCount;
  private final AtomicReference<Instant> lastSquadUpdate = new AtomicReference<>(null);
  private final AtomicReference<Instant> lastEventsFetch = new AtomicReference<>(null);
  private final AtomicReference<Instant> lastFixturesImport = new AtomicReference<>(null);

  private final RequestProcessor.Task refresher;

  public AppStats() {
    // periodic refresh (every 60s)
    refresher = RequestProcessor.getDefault().post(this::refresh, 0, 60_000);
  }

  private void refresh() {
    if (jpa == null) return;
    jpa.tx((EntityManager em) -> {
      playersCount  = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM player").getSingleResult()).intValue();
      clubsCount    = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM club").getSingleResult()).intValue();
      squadsCount   = ((Number) em.createNativeQuery("SELECT COUNT(DISTINCT api_club_id) FROM squad_roster").getSingleResult()).intValue();
      fixturesCount = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM fixture").getSingleResult()).longValue();
      eventsCount   = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM fixture_event").getSingleResult()).longValue();

      // timestamps (NULL-safe casts)
      Object lastRoster = em.createNativeQuery("SELECT MAX(updated) FROM squad_roster").getSingleResult();
      Object lastEv     = em.createNativeQuery("SELECT MAX(fetched_at) FROM fixture_events_raw").getSingleResult();
      Object lastFixImp = em.createNativeQuery(
          "SELECT MAX(COALESCE(imported_at, match_date_utc)) FROM fixture").getSingleResult();

      lastSquadUpdate.set(toInstantOrNull(lastRoster));
      lastEventsFetch.set(toInstantOrNull(lastEv));
      lastFixturesImport.set(toInstantOrNull(lastFixImp));
      return null;
    });
  }

  private static Instant toInstantOrNull(Object o) {
    if (o == null) return null;
    if (o instanceof java.sql.Timestamp ts) return ts.toInstant();
    if (o instanceof java.time.Instant i)  return i;
    if (o instanceof java.util.Date d)     return d.toInstant();
    return null;
  }

  // === MXBean getters/ops ===
  @Override public int getPlayersCount()        { return playersCount; }
  @Override public int getClubsCount()          { return clubsCount; }
  @Override public int getSquadsCount()         { return squadsCount; }
  @Override public long getFixturesCount()      { return fixturesCount; }
  @Override public long getEventsCount()        { return eventsCount; }
  @Override public Instant getLastSquadUpdate() { return lastSquadUpdate.get(); }
  @Override public Instant getLastEventsFetch() { return lastEventsFetch.get(); }
  @Override public Instant getLastFixturesImport() { return lastFixturesImport.get(); }

  @Override public Map<Integer, Long> getFixturesByLeagueForSeason(int season) {
    Map<Integer, Long> out = new HashMap<>();
    if (jpa == null) return out;
    return jpa.tx((EntityManager em) -> {
      var rows = em.createNativeQuery("""
          SELECT league_id, COUNT(*) 
          FROM fixture 
          WHERE season = :s 
          GROUP BY league_id
        """).setParameter("s", season).getResultList();
      for (Object row : rows) {
        Object[] r = (Object[]) row;
        out.put(((Number) r[0]).intValue(), ((Number) r[1]).longValue());
      }
      return out;
    });
  }

  @Override public void refreshNow() { RequestProcessor.getDefault().post(this::refresh); }
  @Override public String ping()     { return "OK " + Instant.now(); }
}

