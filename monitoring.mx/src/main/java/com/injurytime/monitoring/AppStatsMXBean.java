/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// module: monitoring.mx  (package com.injurytime.monitoring.mx)
package com.injurytime.monitoring;

import java.util.Date;
import java.util.Map;

public interface AppStatsMXBean {
  // simple counts
  int  getPlayersCount();
  int  getClubsCount();
  int  getSquadsCount();
  long getFixturesCount();
  long getEventsCount();

  // timestamps as Date (JMX-friendly)
  Date getLastSquadUpdate();
  Date getLastEventsFetch();
  Date getLastFixturesImport();

  // use String keys for JMX-friendliness
  Map<String, Long> getFixturesByLeagueForSeason(int season);

  // ops
  void refreshNow();
  String ping();
}


