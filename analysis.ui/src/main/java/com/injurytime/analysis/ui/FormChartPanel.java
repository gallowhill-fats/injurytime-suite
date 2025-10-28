/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.ui;

import com.injurytime.analysis.api.FormPoint;
import com.injurytime.analysis.api.FormService;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Date;
import org.knowm.xchart.*;
import org.knowm.xchart.style.markers.None;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

public final class FormChartPanel extends JPanel {
  private final XChartPanel<XYChart> xp;
  private final XYChart chart;

  // cache original data so toggling visibility can re-add series
  private static final class SeriesData {
    final List<? extends Number> x; final List<? extends Number> y;
    SeriesData(List<? extends Number> x, List<? extends Number> y) { this.x = x; this.y = y; }
  }
  private final java.util.Map<String, SeriesData> cache = new java.util.LinkedHashMap<>();

  public FormChartPanel(String title) {
    XYChart c = new XYChartBuilder()
        .width(900).height(500).title(title)
        .xAxisTitle("Week").yAxisTitle("Rolling Mean")
        .build();
    c.getStyler().setLegendVisible(true);
    c.getStyler().setMarkerSize(4);
    this.chart = c;
    this.xp = new XChartPanel<>(chart);
    setLayout(new BorderLayout());
    add(xp, BorderLayout.CENTER);
  }

  public void addSeries(String name, List<? extends Number> x, List<? extends Number> y) {
    cache.put(name, new SeriesData(x, y));
    if (chart.getSeriesMap().containsKey(name)) chart.removeSeries(name);
    var s = chart.addSeries(name, x, y);
    s.setMarker(new org.knowm.xchart.style.markers.None());
    xp.revalidate();
    xp.repaint();
  }

  /** Hide or show a series without losing data. */
  public void setSeriesVisible(String name, boolean visible) {
    boolean present = chart.getSeriesMap().containsKey(name);
    if (visible && !present) {
      SeriesData sd = cache.get(name);
      if (sd != null) {
        var s = chart.addSeries(name, sd.x, sd.y);
        s.setMarker(new org.knowm.xchart.style.markers.None());
      }
    } else if (!visible && present) {
      chart.removeSeries(name);
    }
    xp.revalidate();
    xp.repaint();
  }

  /** Remove from chart AND cache (hard delete). */
  public void removeSeries(String name) {
    chart.removeSeries(name);
    cache.remove(name);
    xp.revalidate();
    xp.repaint();
  }

  public void clearAll() {
  // copy keys to avoid ConcurrentModificationException
  java.util.List<String> names = new java.util.ArrayList<>(chart.getSeriesMap().keySet());
  for (String n : names) {
    chart.removeSeries(n);
  }
  xp.revalidate();
  xp.repaint();
}

  
  public void repaintChart() {
    xp.revalidate();
    xp.repaint();
  }
  
  public void setTitle(String title) {
  xp.getChart().setTitle(title);
  xp.revalidate();
  xp.repaint();
}
}






