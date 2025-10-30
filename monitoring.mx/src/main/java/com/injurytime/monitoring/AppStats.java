/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.monitoring;

import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.openide.util.RequestProcessor;

import java.time.Instant;

import java.time.Instant;

import java.time.Instant;

import java.time.Instant;

public final class AppStats implements AppStatsMXBean {
  private volatile int  playersCount;
  private volatile int  clubsCount;
  private volatile int  squadsCount;
  private volatile long fixturesCount;
  private volatile long eventsCount;

  private final AtomicReference<Instant> lastSquadUpdate     = new AtomicReference<>();
  private final AtomicReference<Instant> lastEventsFetch     = new AtomicReference<>();
  private final AtomicReference<Instant> lastFixturesImport  = new AtomicReference<>();

  private final RequestProcessor.Task refresher;

  private final JpaAccess jpa;

  public AppStats(JpaAccess jpa) {
    this.jpa = jpa; // <- inject from Lookup (see Installer)
    // periodic refresh (every 60s)
    this.refresher = RequestProcessor.getDefault().post(this::refresh, 0, 60_000);
  }

  private void refresh() {
    if (jpa == null) return;
    jpa.tx((EntityManager em) -> {
      playersCount  = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM player").getSingleResult()).intValue();
      clubsCount    = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM club").getSingleResult()).intValue();
      squadsCount   = ((Number) em.createNativeQuery("SELECT COUNT(DISTINCT api_club_id) FROM squad_roster").getSingleResult()).intValue();
      fixturesCount = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM fixture").getSingleResult()).longValue();
      eventsCount   = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM fixture_event").getSingleResult()).longValue();

      Object lastRoster = em.createNativeQuery("SELECT MAX(updated) FROM squad_roster").getSingleResult();
      Object lastEv     = em.createNativeQuery("SELECT MAX(fetched_at) FROM fixture_events_raw").getSingleResult();
      Object lastFixImp = em.createNativeQuery("SELECT MAX(COALESCE(imported_at, match_date_utc)) FROM fixture").getSingleResult();

      lastSquadUpdate.set(toInstantOrNull(lastRoster));
      lastEventsFetch.set(toInstantOrNull(lastEv));
      lastFixturesImport.set(toInstantOrNull(lastFixImp));
      return null;
    });
  }

  private static Instant toInstantOrNull(Object o) {
    if (o == null) return null;
    if (o instanceof java.sql.Timestamp ts) return ts.toInstant();
    if (o instanceof java.util.Date d)     return d.toInstant();
    if (o instanceof Instant i)            return i;
    return null;
  }

  // === MXBean getters/ops ===
  @Override public int  getPlayersCount()        { return playersCount; }
  @Override public int  getClubsCount()          { return clubsCount; }
  @Override public int  getSquadsCount()         { return squadsCount; }
  @Override public long getFixturesCount()       { return fixturesCount; }
  @Override public long getEventsCount()         { return eventsCount; }

  @Override public Date getLastSquadUpdate()     { var i = lastSquadUpdate.get();    return (i==null? null : Date.from(i)); }
  @Override public Date getLastEventsFetch()     { var i = lastEventsFetch.get();    return (i==null? null : Date.from(i)); }
  @Override public Date getLastFixturesImport()  { var i = lastFixturesImport.get(); return (i==null? null : Date.from(i)); }

  @Override public Map<String, Long> getFixturesByLeagueForSeason(int season) {
    if (jpa == null) return Map.of();
    return jpa.tx((EntityManager em) -> {
      Map<String, Long> out = new LinkedHashMap<>();
      var rows = em.createNativeQuery("""
          SELECT league_id, COUNT(*) 
          FROM fixture 
          WHERE season = :s 
          GROUP BY league_id
          ORDER BY league_id
        """).setParameter("s", season).getResultList();
      for (Object row : rows) {
        Object[] r = (Object[]) row;
        out.put(String.valueOf(((Number) r[0]).intValue()), ((Number) r[1]).longValue());
      }
      return out;
    });
  }

  @Override public void refreshNow() { RequestProcessor.getDefault().post(this::refresh); }
  @Override public String ping()     { return "OK " + Instant.now(); }
}

