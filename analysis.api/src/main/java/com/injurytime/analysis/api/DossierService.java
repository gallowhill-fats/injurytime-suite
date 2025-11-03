/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

import java.util.List;
import java.util.Map;

public interface DossierService {

  /** Load a full dossier for each fixtureId (league+season inferred from DB). */
  Map<Integer, FixtureDossier> loadDossiers(List<Integer> fixtureIds);

  // === DTOs ===
  record FixtureDossier(
      int fixtureId,
      int leagueId,
      int season,
      int homeTeamId,
      int awayTeamId,
      String homeName,
      String awayName,
      java.time.Instant kickUtc,
      List<StatRow>   teamStatsHome,
      List<StatRow>   teamStatsAway,
      List<WeekPoint> last5Home,
      List<WeekPoint> last5Away,
      List<ScoreBin>  histHome,
      List<ScoreBin>  histAway,
      List<ScoreBin>  histLeague,
      List<MatchNote> last5NotesHome,
      List<MatchNote> last5NotesAway,
      List<Unavailable> unavailableHome,
      List<Unavailable> unavailableAway,
      List<Scorer> topScorersHome,
      List<Scorer> topScorersAway
  ) {}

  /** Code = stat key (“n”, “p”, “ppg”, …), cnt, % and per-game. */
  record StatRow(String code, Integer count, Double pct, Double perGame) {}

  /** For “rolling form” or last-5 3-1-0 points per week. */
  record WeekPoint(Integer week, double value) {}

  /** Score histogram bin (e.g., “1-0”, 14) — with a special label “other”. */
  record ScoreBin(String label, int count) {}

  /** Free-text lines for last-5 summaries (scorers, times, reds). */
  record MatchNote(String line) {}

  /** Unavailability tuple. */
  record Unavailable(int playerId, String playerName, String type, String status, String notes) {}

  /** Top scorers (season to date, simple count). */
  record Scorer(int playerId, String playerName, int goals) {}

}

