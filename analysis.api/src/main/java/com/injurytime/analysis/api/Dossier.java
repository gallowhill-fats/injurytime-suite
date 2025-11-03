/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

import java.util.List;

public record Dossier(
  long fixtureId,
  int leagueId, int season,
  TeamBlock home, TeamBlock away,
  List<ScoreBin> homeHist, List<ScoreBin> awayHist, List<ScoreBin> leagueHist,
  List<FormPoint> lastNPointsHome, List<FormPoint> lastNPointsAway,
  List<MatchSummary> last5Home, List<MatchSummary> last5Away
) {}
