/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

import java.util.List;
import java.util.Map;

public interface FormService {
  Map<Integer, List<WeekPoint>> leagueRollingForm(int leagueId, int season, int window);
  
  Map<Integer, String> teamLabels(int leagueId, int season);

  /** Rolling form for one team (window N, optional max week). */
  List<WeekPoint> teamRollingForm(int leagueId, int season, int teamApiId, int window, Integer maxWeek);
}


