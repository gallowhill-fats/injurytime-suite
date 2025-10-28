/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

public interface FormMetric {
  /** Human name for UI. */
  String id();
  /** Score for a single match row. Higher = better recent form. */
  double score(TeamMatchRow m);
}

