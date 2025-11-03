/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

/** Code = stat key (“n”, “p”, “ppg”, …), cnt, % and per-game. */
  record StatRow(String code, Integer count, Double pct, Double perGame) {}
