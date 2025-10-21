/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.pg.tools.squad.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbPreferences;

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
      
      final String key = "squad-importer-pg-chooser-v5";
    resetChooserDir(key);

    File tmp = new File("D:\\sdata");
    if (!tmp.isDirectory()) {
        tmp = new File(System.getProperty("user.home"));
    }
    final File start = tmp; // final, no reassignment later

    File file = new FileChooserBuilder(key)
            .setTitle("Select Squad JSON")
            .setDefaultWorkingDirectory(start)
            .setFilesOnly(true)
            .showOpenDialog();

    if (file == null) return;
    Logger.getLogger(getClass().getName()).info("Picked: " + file.getAbsolutePath());
  }
  
  private static void resetChooserDir(String key) {
    try {
        // FileChooserBuilder stores last-used dir under a node named by your key
        Preferences root = NbPreferences.forModule(FileChooserBuilder.class);
        if (root.nodeExists(key)) {
            root.node(key).removeNode();   // delete the node for this chooser
            root.flush();
        }
    } catch (Exception ex) {
        Logger.getLogger("chooser").fine("Could not reset chooser dir for key=" + key + ": " + ex.getMessage());
        // It's safe to ignore; worst case NB keeps the old dir.
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

