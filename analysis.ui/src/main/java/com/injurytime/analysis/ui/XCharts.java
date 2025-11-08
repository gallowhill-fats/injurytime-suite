/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.ui;

import java.awt.Color;
import org.knowm.xchart.*;
import org.knowm.xchart.style.markers.None;

import javax.swing.*;
import java.util.List;
import org.knowm.xchart.style.CategoryStyler;

// imports you likely already have:
// import org.knowm.xchart.*;
// import org.knowm.xchart.style.markers.None;
// import java.awt.Color;

final class XCharts {

  static JPanel line(String title, String xLabel, String yLabel,
                     List<? extends Number> x1, List<? extends Number> y1, String s1,
                     List<? extends Number> x2, List<? extends Number> y2, String s2) {
    XYChart c = new XYChartBuilder().width(700).height(300).title(title)
        .xAxisTitle(xLabel).yAxisTitle(yLabel).build();
    var a = c.addSeries(s1, x1, y1); a.setMarker(new None());
    var b = c.addSeries(s2, x2, y2); b.setMarker(new None());
    return new XChartPanel<>(c);
  }

  // OLD histogram kept for reference
  static JPanel bars(String title, List<String> bins, List<? extends Number> counts) {
    CategoryChart c = new CategoryChartBuilder().width(700).height(300).title(title)
        .xAxisTitle("Score").yAxisTitle("Count").build();
    c.addSeries("freq", bins, counts);
    c.getStyler().setXAxisLabelRotation(45);
    return new XChartPanel<>(c);
  }
  
  // add this in your XCharts class
static JPanel barsHomeAwayStacked(String title,
                                  java.util.List<String> scoreBins,
                                  java.util.List<? extends Number> homeCounts,
                                  java.util.List<? extends Number> awayCounts) {
  var c = new org.knowm.xchart.CategoryChartBuilder()
      .width(800).height(340).title(title)
      .xAxisTitle("Score").yAxisTitle("Frequency")
      .build();
  c.getStyler().setStacked(true);
  c.getStyler().setLegendVisible(true);
  c.getStyler().setToolTipsEnabled(true);
  enableAnnotations(c.getStyler(), true);
  c.getStyler().setXAxisLabelRotation(30);
  c.getStyler().setAvailableSpaceFill(0.9);
  c.getStyler().setSeriesColors(new java.awt.Color[]{
      new java.awt.Color(52,120,246), // Home
      new java.awt.Color(246,92,104)  // Away
  });
  c.addSeries("Home", scoreBins, homeCounts);
  c.addSeries("Away", scoreBins, awayCounts);
  return new org.knowm.xchart.XChartPanel<>(c);
}

// put this in the same class where you build charts
private static void enableAnnotations(Object styler, boolean enabled) {
  try {
    styler.getClass().getMethod("setHasAnnotations", boolean.class).invoke(styler, enabled);
  } catch (NoSuchMethodException e1) {
    try {
      styler.getClass().getMethod("setAnnotationsEnabled", boolean.class).invoke(styler, enabled);
    } catch (Exception ignore) { /* older XChart without annotations */ }
  } catch (Exception ignore) { /* reflection failed — ignore */ }
}



}


