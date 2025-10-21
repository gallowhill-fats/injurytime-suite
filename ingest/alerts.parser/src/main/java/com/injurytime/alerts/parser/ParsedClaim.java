/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.alerts.parser;

import java.time.LocalDate;

public record ParsedClaim(
  String playerName, String clubName,
  String status, String type, String reason,
  LocalDate startDate, LocalDate expectedReturn,
  Integer expectedDays, int confidence,
  String headline, String snippet, String url) {}

