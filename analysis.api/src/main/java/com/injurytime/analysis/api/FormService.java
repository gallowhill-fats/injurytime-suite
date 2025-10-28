/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

import java.util.List;
import java.util.Map;

public interface FormService {
  /** All-team rolling form for a league/season and window N. */
  Map<Integer, List<WeekPoint>> leagueRollingForm(int leagueId, int season, int window);

  /** Team labels (abbr or name) keyed by api id. */
  Map<Integer, String> teamLabels(int leagueId, int season);
}

