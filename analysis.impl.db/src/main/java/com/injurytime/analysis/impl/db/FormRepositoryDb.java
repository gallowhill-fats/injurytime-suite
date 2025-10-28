/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.impl.db;

import com.injurytime.analysis.api.*;
import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.openide.util.lookup.ServiceProvider;
import java.util.*;
import org.openide.util.Lookup;

@ServiceProvider(service = FormRepository.class)
public final class FormRepositoryDb implements FormRepository {
  private final JpaAccess jpa; // from storage.api
  
  String sql = """
  SELECT r.match_date_utc, r.pts
  FROM v_team_match_row r
  JOIN fixture f ON f.fixture_id = r.fixture_id
  WHERE r.league_id   = :lid
    AND r.season      = :season
    AND r.team_api_id = :tid
    AND is_finished(r.status_short)
    AND fixture_week_no(f.round_name) <= COALESCE(:maxWeek, 99999)
  ORDER BY r.match_date_utc DESC
  LIMIT :lim
""";

  public FormRepositoryDb() { this.jpa = Lookup.getDefault().lookup(JpaAccess.class); }

 
  @Override
public List<FormPoint> loadTeamForm(int leagueId, int season, int teamApiId, int limit, Integer maxWeek) {
  return jpa.tx(em -> {
    var q = em.createNativeQuery("""
  SELECT r.match_date_utc, r.pts
  FROM v_team_match_row r
  JOIN fixture f ON f.fixture_id = r.fixture_id
  WHERE r.league_id = :lid
    AND r.season = :season
    AND r.team_api_id = :tid
    AND is_finished(r.status_short)
    AND ( CAST(:maxWeek AS INTEGER) IS NULL
          OR fixture_week_no(f.round_name) <= CAST(:maxWeek AS INTEGER) )
  ORDER BY r.match_date_utc DESC
  LIMIT :lim
""");
    q.setParameter("lid", leagueId);
    q.setParameter("season", season);
    q.setParameter("tid", teamApiId);
    q.setParameter("lim", limit);
    q.setParameter("maxWeek", maxWeek);  

    @SuppressWarnings("unchecked")
    var rows = (List<Object[]>) q.getResultList();

    // Map & sort ascending for rolling computations later
    return rows.stream()
        .map(r -> new FormPoint(toInstant(r[0]), ((Number) r[1]).doubleValue()))
        .sorted(java.util.Comparator.comparing(FormPoint::when))
        .toList();
  });
}
  
  private static Instant toInstant(Object x) {
  if (x == null) return null;
  if (x instanceof Instant i)          return i;
  if (x instanceof Timestamp ts)       return ts.toInstant();
  if (x instanceof Date d)             return d.toInstant();
  if (x instanceof OffsetDateTime odt) return odt.toInstant();
  if (x instanceof LocalDateTime ldt)  return ldt.toInstant(ZoneOffset.UTC); // adjust if you prefer system TZ
  if (x instanceof Long millis)        return Instant.ofEpochMilli(millis);
  // last resort: try parsing
  return Instant.parse(x.toString());
}
}



