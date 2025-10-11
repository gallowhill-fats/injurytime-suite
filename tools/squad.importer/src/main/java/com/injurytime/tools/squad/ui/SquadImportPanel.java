/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.tools.squad.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class SquadImportPanel extends JPanel {
  private final JTextField seasonField = new JTextField(12);
  private final JTextField filesField  = new JTextField();
  private final JButton browseBtn      = new JButton("Browse…");

  private List<File> selectedFiles = new ArrayList<>();

  public SquadImportPanel() {
    setLayout(new GridBagLayout());
    filesField.setEditable(false);

    var gbc = new GridBagConstraints();
    gbc.insets = new Insets(6,6,6,6);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    gbc.gridx=0; gbc.gridy=0; add(new JLabel("Season label:"), gbc);
    gbc.gridx=1; gbc.gridy=0; gbc.weightx=1; add(seasonField, gbc);

    gbc.gridx=0; gbc.gridy=1; gbc.weightx=0; add(new JLabel("Squad JSON file(s):"), gbc);
    gbc.gridx=1; gbc.gridy=1; gbc.weightx=1; add(filesField, gbc);
    gbc.gridx=2; gbc.gridy=1; gbc.weightx=0; add(browseBtn, gbc);

    browseBtn.addActionListener(e -> openChooser());
  }

  private void openChooser() {
    JFileChooser ch = new JFileChooser();
    ch.setDialogTitle("Select Squad JSON files");
    ch.setMultiSelectionEnabled(true);
    ch.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
    int res = ch.showOpenDialog(this);
    if (res == JFileChooser.APPROVE_OPTION) {
      File[] files = ch.getSelectedFiles();
      selectedFiles = files == null ? List.of() : List.of(files);
      if (selectedFiles.isEmpty()) {
        filesField.setText("");
      } else if (selectedFiles.size() == 1) {
        filesField.setText(selectedFiles.get(0).getAbsolutePath());
      } else {
        filesField.setText(selectedFiles.size() + " files selected");
      }
    }
  }

  public String getSeasonId() {
    String s = seasonField.getText();
    return s == null ? "" : s.trim();
  }

  public List<Path> getSelectedPaths() {
    return selectedFiles.stream().map(File::toPath).collect(Collectors.toList());
  }
}

