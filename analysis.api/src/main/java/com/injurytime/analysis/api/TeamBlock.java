/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

import java.util.List;

/**
 *
 * @author clayton
 */
public record TeamBlock(
  int teamApiId, String name,
  List<StatRow> statsTable,            // "stat", "H value", "A value" formatting done in UI
  List<Scorer> topScorers,
  List<Unavailability> unavailable
) {}
