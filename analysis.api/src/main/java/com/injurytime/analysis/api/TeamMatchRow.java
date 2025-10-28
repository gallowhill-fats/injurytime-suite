/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

import java.time.Instant;

/** A single team match row (one team’s perspective). */
public record TeamMatchRow(
    int leagueId, int season, long fixtureId,
    int teamApiId, int opponentApiId,
    boolean home,
    int gf, int ga,
    Instant matchUtc,
    char result // 'W','D','L'
) {}

