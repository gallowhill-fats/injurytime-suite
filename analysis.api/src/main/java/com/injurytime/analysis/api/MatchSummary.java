/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

import java.util.List;

public record MatchSummary(long fixtureId, java.time.Instant whenUtc,
                           String opponent, boolean home, String scoreText,
                           List<GoalEv> goals, List<CardEv> reds) {}
