/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.tools.squad;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.FileInputStream;
import java.nio.file.Path;

@ActionID(category = "InjuryTime", id = "com.injurytime.tools.squad.ImportSquadJsonAction")
@ActionRegistration(displayName = "Import Squad JSON…")
@ActionReference(path = "Menu/Tools", position = 2050)
public final class ImportSquadJsonAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            // Ask for JSON file
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select Squad JSON file");
            if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
            Path file = fc.getSelectedFile().toPath();

            // Ask for league & season
            String leagueStr = JOptionPane.showInputDialog(null, "LEAGUE_API_ID:", "Enter League API ID", JOptionPane.QUESTION_MESSAGE);
            if (leagueStr == null) return;
            String seasonStr = JOptionPane.showInputDialog(null, "SEASON (e.g., 2025):", "Enter Season", JOptionPane.QUESTION_MESSAGE);
            if (seasonStr == null) return;

            int leagueApiId = Integer.parseInt(leagueStr.trim());
            int season = Integer.parseInt(seasonStr.trim());

            // Get EMF (prefer Lookup if you expose one; fallback to local PU)
            EntityManagerFactory emf = Lookup.getDefault().lookup(EntityManagerFactory.class);
            if (emf == null) {
                emf = Persistence.createEntityManagerFactory("injurytimePU");
            }

            // Run import
            SquadJsonImporter importer = new SquadJsonImporter(emf);
            try (FileInputStream in = new FileInputStream(file.toFile())) {
                importer.importSquad(in, leagueApiId, season);
            }

            JOptionPane.showMessageDialog(null, "Squad imported for LEAGUE_API_ID="
                    + leagueApiId + ", SEASON=" + season);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Import failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

