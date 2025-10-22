/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.impl.db;

import com.injurytime.analysis.api.LeagueTableService;
import com.injurytime.analysis.api.TableRow;
import com.injurytime.storage.api.JpaAccess;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;

@org.openide.util.lookup.ServiceProvider(service = LeagueTableService.class, position = 10)
public final class LeagueTableServiceDb implements LeagueTableService {

  private final JpaAccess jpa;
  @Inject
  public LeagueTableServiceDb(JpaAccess jpa) { this.jpa = jpa; }
  public LeagueTableServiceDb() { this(org.openide.util.Lookup.getDefault().lookup(JpaAccess.class)); }

  @Override
  public List<TableRow> loadTable(int leagueId, int season, Integer weekOrNull) {
    return jpa.tx((EntityManager em) -> {
      var q = em.createNativeQuery("""
        WITH base AS (SELECT * FROM league_table_as_of(:lid,:season,:week)),
        ranked AS (
          SELECT t.*, RANK() OVER(ORDER BY pts DESC, gd DESC, gf DESC) AS rk
          FROM base t
        )
        SELECT rk, c.club_name, played, won, drawn, lost,
               gf_home, ga_home, gf_away, ga_away,
               (gf - ga) AS gd, pts,
               team_last_form(:lid,:season,team_api_id,5,:week) AS form5
        FROM ranked r
        JOIN club c ON c.api_club_id = r.team_api_id
        ORDER BY rk
      """);
      q.setParameter("lid", leagueId);
      q.setParameter("season", season);
      q.setParameter("week", weekOrNull); // can be null

      @SuppressWarnings("unchecked")
      var rows = (List<Object[]>) q.getResultList();

      return rows.stream().map(a -> new TableRow(
        ((Number)a[0]).intValue(),           // rank
        (String)a[1],                        // team
        ((Number)a[2]).intValue(),           // played
        ((Number)a[3]).intValue(),           // won
        ((Number)a[4]).intValue(),           // drawn
        ((Number)a[5]).intValue(),           // lost
        ((Number)a[6]).intValue(),           // gf home
        ((Number)a[7]).intValue(),           // ga home
        ((Number)a[8]).intValue(),           // gf away
        ((Number)a[9]).intValue(),           // ga away
        ((Number)a[10]).intValue(),          // gd
        ((Number)a[11]).intValue(),          // pts
        (String)a[12]                        // form5
      )).toList();
    });
  }
}

