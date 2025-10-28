/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.monitoring;

import java.time.Instant;
import java.util.Map;

public interface AppStatsMXBean {
  // Simple, cached counters
  int getPlayersCount();
  int getClubsCount();
  int getSquadsCount();
  long getFixturesCount();           // all fixtures in DB
  long getEventsCount();

  // Timestamps
  Instant getLastSquadUpdate();
  Instant getLastEventsFetch();
  Instant getLastFixturesImport();   // null if unknown

  // Optional: per-league snapshot (leagueId -> fixtures count this season)
  Map<Integer, Long> getFixturesByLeagueForSeason(int season);

  // Ops
  void refreshNow();                 // refresh cache immediately
  String ping();                     // quick health string
}

