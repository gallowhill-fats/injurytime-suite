/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.impl.db;

import com.injurytime.analysis.api.FormService;
import com.injurytime.analysis.api.WeekPoint;
import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = FormService.class)
public final class FormServiceDb implements FormService {

  private final JpaAccess jpa;

  // NetBeans Lookup will call the no-arg ctor; pull JpaAccess from Lookup inside
  public FormServiceDb() {
    this.jpa = org.openide.util.Lookup.getDefault().lookup(JpaAccess.class);
  }

  // --- already implemented in your class ---
  @Override
  public Map<Integer, List<WeekPoint>> leagueRollingForm(int leagueId, int season, int window) {
    // ... your existing implementation ...
    throw new UnsupportedOperationException("impl present elsewhere");
  }

  @Override
  public Map<Integer, String> teamLabels(int leagueId, int season) {
    // ... your existing implementation ...
    throw new UnsupportedOperationException("impl present elsewhere");
  }

  // --- NEW: per-team rolling form (3-1-0), window over weeks ---
  
  public List<WeekPoint> teamRollingForm(int leagueId, int season, int teamApiId, int window) {
    return teamRollingForm(leagueId, season, teamApiId, window, null);
  }

  // Optional maxWeek filter (e.g., “as of week N”)
  public List<WeekPoint> teamRollingForm(int leagueId, int season, int teamApiId, int window, Integer maxWeek) {
    if (jpa == null) return List.of();

    List<Object[]> rows = jpa.tx((EntityManager em) -> em.createNativeQuery("""
        SELECT fixture_week_no(f.round_name) AS wk,
               r.pts                          AS pts
        FROM v_team_match_row r
        JOIN fixture f ON f.fixture_id = r.fixture_id
        WHERE r.league_id = :lid
          AND r.season    = :season
          AND r.team_api_id = :tid
          AND is_finished(r.status_short)
          AND (:maxw IS NULL OR fixture_week_no(f.round_name) <= :maxw)
        ORDER BY wk
      """)
      .setParameter("lid", leagueId)
      .setParameter("season", season)
      .setParameter("tid", teamApiId)
      .setParameter("maxw", maxWeek)
      .getResultList());

    // Compute rolling mean over the last `window` matches (by week)
    List<WeekPoint> out = new ArrayList<>();
    Deque<Integer> buf = new ArrayDeque<>(Math.max(1, window));
    int sum = 0;
    Integer lastWeek = null;

    for (Object[] r : rows) {
      Integer wk  = (r[0] == null ? null : ((Number) r[0]).intValue());
      Integer pts = (r[1] == null ? 0    : ((Number) r[1]).intValue());
      if (wk == null) continue;

      // If multiple matches end up in same “week” label (rare), treat each match sequentially.
      buf.addLast(pts);
      sum += pts;
      if (buf.size() > window) {
        sum -= buf.removeFirst();
      }
      double mean = sum / (double) buf.size();

      // Only append once per week label (if duplicates, keep the last one in that week)
      if (Objects.equals(lastWeek, wk) && !out.isEmpty()) {
        out.set(out.size() - 1, new WeekPoint(wk, mean));
      } else {
        out.add(new WeekPoint(wk, mean));
      }
      lastWeek = wk;
    }

    return out;
  }
}
