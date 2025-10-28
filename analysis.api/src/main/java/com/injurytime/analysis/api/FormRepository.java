/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

import java.util.List;

public interface FormRepository {
  /** Returns per-match points (3/1/0 or whatever your metric) newest-first. */
  List<FormPoint> loadTeamForm(int leagueId, int season, int teamApiId, int limit, Integer maxWeek);
}






