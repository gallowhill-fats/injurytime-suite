/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

final class Accordion extends JPanel {
  static final class Section {
    final JToggleButton header;
    final JPanel body;
    Section(String title, JComponent content) {
      setUIFont(headerFont());
      header = new JToggleButton(title, true);
      header.setFocusPainted(false);
      header.setHorizontalAlignment(SwingConstants.LEFT);
      header.putClientProperty("JButton.buttonType", "segmented");
      body = new JPanel(new BorderLayout());
      body.add(content, BorderLayout.CENTER);
    }
  }

  private final List<Section> sections = new ArrayList<>();

  Accordion() {
    super();
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  }

  public void addSection(String title, JComponent content) {
    Section s = new Section(title, content);
    s.header.addActionListener(e -> {
      s.body.setVisible(s.header.isSelected());
      revalidate(); repaint();
    });
    JPanel wrap = new JPanel(new BorderLayout());
    wrap.add(s.header, BorderLayout.NORTH);
    wrap.add(s.body, BorderLayout.CENTER);
    sections.add(s);
    add(wrap);
  }

  private static Font headerFont() {
    return UIManager.getFont("Label.font").deriveFont(Font.BOLD, 14f);
  }

  private static void setUIFont(Font f) {}
}

