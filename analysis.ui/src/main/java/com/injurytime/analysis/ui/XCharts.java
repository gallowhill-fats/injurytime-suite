/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.ui;

import org.knowm.xchart.*;
import org.knowm.xchart.style.markers.None;

import javax.swing.*;
import java.util.List;

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

  static JPanel bars(String title, List<String> bins, List<? extends Number> counts) {
    CategoryChart c = new CategoryChartBuilder().width(700).height(300).title(title)
        .xAxisTitle("Score").yAxisTitle("Count").build();
    c.addSeries("freq", bins, counts);
    c.getStyler().setXAxisLabelRotation(45);
    return new XChartPanel<>(c);
  }
}

