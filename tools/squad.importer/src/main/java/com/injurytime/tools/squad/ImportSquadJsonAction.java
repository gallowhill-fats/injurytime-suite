/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.tools.squad;

import com.injurytime.storage.api.JpaAccess;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;

@ActionID(category="Tools", id="com.injurytime.tools.squad.ImportSquadJsonAction")
@ActionRegistration(displayName="Import Squad JSON…")
@ActionReference(path="Menu/Tools", position=1200)
public final class ImportSquadJsonAction implements ActionListener {

  @Override public void actionPerformed(ActionEvent e) {
    // 1) Pick file
    JFileChooser ch = new JFileChooser();
    ch.setDialogTitle("Choose squad JSON file");
    if (ch.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
    File sel = ch.getSelectedFile();
    if (sel == null) return;

    // 2) Ask season id
    String s = JOptionPane.showInputDialog(null, "Season ID (e.g., 2025):", "Season", JOptionPane.QUESTION_MESSAGE);
    if (s == null || s.isBlank()) return;
    String seasonId;
    String season = JOptionPane.showInputDialog(null, "Season (e.g. 2024):",
                                            "Season", JOptionPane.QUESTION_MESSAGE);
    if (season == null || season.isBlank()) return;

    

    // 3) Lookup JPA and import
    JpaAccess jpa = Lookup.getDefault().lookup(JpaAccess.class);
    if (jpa == null) {
      DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(
          "Storage/JPA module not available.", NotifyDescriptor.ERROR_MESSAGE));
      return;
    }

    try {
      SquadJsonImporter importer = new SquadJsonImporter(jpa); // ✔ matches constructor above
      int n = importer.importFile(Path.of(sel.toURI()), season.trim());
      DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(
          "Imported/updated " + n + " squad rows.", NotifyDescriptor.INFORMATION_MESSAGE));
    } catch (Exception ex) {
      DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(
          "Import failed: " + ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE));
    }
  }
}
