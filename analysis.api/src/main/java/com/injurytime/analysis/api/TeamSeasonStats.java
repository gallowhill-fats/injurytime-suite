/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

import java.time.Instant;

public record TeamSeasonStats(
    int leagueId, int season, int teamApiId,
    int matches, int pts, int w, int d, int l,
    int gf, int ga,
    double avgGf, double avgGa,
    int homeGf, int homeGa, int awayGf, int awayGa,
    int cleanSheets, int over25, int under25,
    Integer h1Goals, Integer h2Goals,
    String last5WDL, Integer last5Points
) {}



