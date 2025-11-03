/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;

final class PrintUtil {

  static void printComponent(Component comp) {
    PrinterJob job = PrinterJob.getPrinterJob();
    job.setJobName("Fixture Dossier");
    job.setPrintable((graphics, pageFormat, pageIndex) -> {
      if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
      Graphics2D g2 = (Graphics2D) graphics;
      g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
      double sx = pageFormat.getImageableWidth()  / comp.getWidth();
      double sy = pageFormat.getImageableHeight() / comp.getHeight();
      double scale = Math.min(sx, sy);
      g2.scale(scale, scale);
      comp.printAll(g2);
      return Printable.PAGE_EXISTS;
    });
    if (job.printDialog()) {
      try { job.print(); } catch (PrinterException ignored) {}
    }
  }
}

