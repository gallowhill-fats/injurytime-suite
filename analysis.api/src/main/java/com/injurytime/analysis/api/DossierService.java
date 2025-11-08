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

      // team stats + last5
      java.util.List<StatRow>   teamStatsHome,
      java.util.List<StatRow>   teamStatsAway,
      java.util.List<WeekPoint> last5Home,
      java.util.List<WeekPoint> last5Away,

      // legacy histograms (kept for compatibility)
      java.util.List<ScoreBin>  histHome,
      java.util.List<ScoreBin>  histAway,
      java.util.List<ScoreBin>  histLeague,

      // ✅ NEW: venue-split histograms (6 lists)
      java.util.List<ScoreBin>  homeHistAtHome,
      java.util.List<ScoreBin>  homeHistAway,
      java.util.List<ScoreBin>  awayHistAtHome,
      java.util.List<ScoreBin>  awayHistAway,
      java.util.List<ScoreBin>  leagueHistHome,
      java.util.List<ScoreBin>  leagueHistAway,

      // notes, availability, scorers
      java.util.List<MatchNote>    last5NotesHome,
      java.util.List<MatchNote>    last5NotesAway,
      java.util.List<Unavailable>  unavailableHome,
      java.util.List<Unavailable>  unavailableAway,
      java.util.List<Scorer>       topScorersHome,
      java.util.List<Scorer>       topScorersAway
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

