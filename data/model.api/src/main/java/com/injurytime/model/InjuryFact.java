/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.model;

import java.time.*;

public record InjuryFact(
  String playerId, String playerName,
  String teamId, String teamName,
  String injuryType, String bodyPart,
  String severity, String status,
  LocalDate injuryDate, LocalDate expectedReturn,
  String sourceUrl, String sourceTitle,
  Instant sourcePublishedAt, double confidence
) {}

