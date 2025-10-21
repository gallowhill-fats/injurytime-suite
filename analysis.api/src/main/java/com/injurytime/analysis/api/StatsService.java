/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

public interface StatsService {
  TeamSeasonStats loadTeamSeasonStats(int leagueId, int season, int teamApiId);
  java.util.List<TeamSeasonStats> loadLeagueTableStats(int leagueId, int season);
}
