/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.impl.db;

import com.injurytime.analysis.api.FormPoint;
import com.injurytime.analysis.api.FormRepository;
import com.injurytime.analysis.api.FormService;
import com.injurytime.analysis.api.WeekPoint;
import com.injurytime.storage.api.JpaAccess;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = FormService.class)
public final class FormServiceDb implements FormService {
  private final JpaAccess jpa;
  private final FormRepository repo = Lookup.getDefault().lookup(FormRepository.class);
  
  
  public FormServiceDb() {
    this(Lookup.getDefault().lookup(JpaAccess.class));
  }

  /** Testable ctor */
  FormServiceDb(JpaAccess jpa) {
    this.jpa = Objects.requireNonNull(jpa, "JpaAccess not found in Lookup");
  }
  
  @Override
public Map<Integer, List<WeekPoint>> leagueRollingForm(int leagueId, int season, int window) {
  return jpa.tx(em -> {
    var q = em.createNativeQuery("""
      WITH weeks AS (
        SELECT r.team_api_id,
               fixture_week_no(f.round_name) AS week_no,
               CASE WHEN r.res='W' THEN 3 WHEN r.res='D' THEN 1 ELSE 0 END AS pts
        FROM v_team_match_row r
        JOIN fixture f ON f.fixture_id = r.fixture_id
        WHERE r.league_id = :lid AND r.season = :season AND is_finished(r.status_short)
      ),
      agg AS (
        SELECT w1.team_api_id, w1.week_no,
               AVG(w2.pts)::float AS roll
        FROM weeks w1
        JOIN weeks w2
          ON w2.team_api_id = w1.team_api_id
         AND w2.week_no BETWEEN w1.week_no - :win + 1 AND w1.week_no
        GROUP BY w1.team_api_id, w1.week_no
      )
      SELECT a.team_api_id, a.week_no, a.roll
      FROM agg a
      ORDER BY a.team_api_id, a.week_no
    """);
    q.setParameter("lid", leagueId);
    q.setParameter("season", season);
    q.setParameter("win", window);

    @SuppressWarnings("unchecked")
    List<Object[]> rows = q.getResultList();

    Map<Integer, List<WeekPoint>> out = new LinkedHashMap<>();
    for (Object[] r : rows) {
      Integer tid  = ((Number) r[0]).intValue();
      Integer wk   = ((Number) r[1]).intValue();
      double roll  = ((Number) r[2]).doubleValue();
      out.computeIfAbsent(tid, k -> new ArrayList<>()).add(new WeekPoint(wk, roll));
    }
    return out;
  });
}

@Override
public Map<Integer, String> teamLabels(int leagueId, int season) {
  return jpa.tx(em -> {
    var q = em.createNativeQuery("""
      SELECT DISTINCT ft.home_team_id AS team_api_id, COALESCE(c.club_abbr, c.club_name, ('T'||ft.home_team_id)) AS label
      FROM fixture_teams ft
      JOIN fixture f ON f.fixture_id = ft.fixture_id
      LEFT JOIN club c ON c.api_club_id = ft.home_team_id
      WHERE f.league_id = :lid AND f.season = :season
      UNION
      SELECT DISTINCT ft.away_team_id AS team_api_id, COALESCE(c.club_abbr, c.club_name, ('T'||ft.away_team_id)) AS label
      FROM fixture_teams ft
      JOIN fixture f ON f.fixture_id = ft.fixture_id
      LEFT JOIN club c ON c.api_club_id = ft.away_team_id
      WHERE f.league_id = :lid AND f.season = :season
    """);
    q.setParameter("lid", leagueId);
    q.setParameter("season", season);

    @SuppressWarnings("unchecked")
    List<Object[]> rows = q.getResultList();
    Map<Integer, String> labels = new HashMap<>();
    for (Object[] r : rows) labels.put(((Number) r[0]).intValue(), (String) r[1]);
    return labels;
  });
}
}