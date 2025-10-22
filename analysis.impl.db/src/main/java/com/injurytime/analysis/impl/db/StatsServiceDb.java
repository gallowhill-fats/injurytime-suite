/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.impl.db;

import com.injurytime.analysis.api.*;
import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;

import java.util.*;
import java.util.function.Function;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;


@ServiceProvider(service = StatsService.class, position = 100)
public final class StatsServiceDb implements StatsService {
  private final JpaAccess jpa;

  public StatsServiceDb() {
        this.jpa = Lookup.getDefault().lookup(JpaAccess.class);
        if (this.jpa == null) {
            throw new IllegalStateException("JpaAccess not found in Lookup. Did you register its impl?");
        }
    }

  @Override
public TeamSeasonStats loadTeamSeasonStats(int leagueId, int season, int teamApiId) {
    return jpa.<TeamSeasonStats>tx(em -> mapTeam(em, leagueId, season, teamApiId));
}

  @Override
  public List<TeamSeasonStats> loadLeagueTableStats(int leagueId, int season) {
    return jpa.tx(em -> {
      @SuppressWarnings("unchecked")
      List<Number> teams = em.createNativeQuery("""
          SELECT DISTINCT team_api_id
          FROM v_team_season_agg
          WHERE league_id=:lid AND season=:s
          """)
          .setParameter("lid", leagueId).setParameter("s", season).getResultList();
      var out = new ArrayList<TeamSeasonStats>(teams.size());
      for (Number n : teams) out.add(mapTeam(em, leagueId, season, n.intValue()));
      // sort by points, GD, GF
      out.sort(Comparator.<TeamSeasonStats>comparingInt(ts -> -ts.pts())
         .thenComparingInt(ts -> -(ts.gf() - ts.ga()))
         .thenComparingInt(ts -> -ts.gf()));
      return out;
    });
  }

  private TeamSeasonStats mapTeam(EntityManager em, int leagueId, int season, int teamId) {
    Object[] a = (Object[]) em.createNativeQuery("""
      SELECT matches, pts, w, d, l, gf, ga,
             avg_gf, avg_ga, home_gf, home_ga, away_gf, away_ga,
             clean_sheets, over2_5, under2_5
      FROM v_team_season_agg
      WHERE league_id=:lid AND season=:s AND team_api_id=:tid
    """).setParameter("lid", leagueId).setParameter("s", season).setParameter("tid", teamId)
      .getSingleResult();

    Object[] h = (Object[]) em.createNativeQuery("""
      SELECT h1_goals, h2_goals
      FROM v_team_season_halves
      WHERE league_id=:lid AND season=:s AND team_api_id=:tid
    """).setParameter("lid", leagueId).setParameter("s", season).setParameter("tid", teamId)
      .getResultStream().findFirst().orElse(new Object[]{null,null});

    Object[] l5 = (Object[]) em.createNativeQuery("""
      SELECT last5_wdl, last5_points
      FROM v_team_last5
      WHERE league_id=:lid AND season=:s AND team_api_id=:tid
    """).setParameter("lid", leagueId).setParameter("s", season).setParameter("tid", teamId)
      .getResultStream().findFirst().orElse(new Object[]{null,null});

    return new TeamSeasonStats(
      leagueId, season, teamId,
      ((Number)a[0]).intValue(), ((Number)a[1]).intValue(),
      ((Number)a[2]).intValue(), ((Number)a[3]).intValue(), ((Number)a[4]).intValue(),
      ((Number)a[5]).intValue(), ((Number)a[6]).intValue(),
      ((Number)a[7]).doubleValue(), ((Number)a[8]).doubleValue(),
      ((Number)a[9]).intValue(), ((Number)a[10]).intValue(),
      ((Number)a[11]).intValue(), ((Number)a[12]).intValue(),
      ((Number)a[13]).intValue(), ((Number)a[14]).intValue(), ((Number)a[15]).intValue(),
      (h[0] == null ? null : ((Number)h[0]).intValue()),
      (h[1] == null ? null : ((Number)h[1]).intValue()),
      (String) l5[0],
      (l5[1] == null ? null : ((Number) l5[1]).intValue())
    );
  }
}

