/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.metrics;

import com.injurytime.analysis.api.FormMetric;
import com.injurytime.analysis.api.TeamMatchRow;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = FormMetric.class)
public final class Points310Metric implements FormMetric {
  @Override public String id() { return "Points (3/1/0)"; }
  @Override public double score(TeamMatchRow m) {
    return switch (m.result()) {
      case 'W' -> 3.0;
      case 'D' -> 1.0;
      default  -> 0.0;
    };
  }
}

