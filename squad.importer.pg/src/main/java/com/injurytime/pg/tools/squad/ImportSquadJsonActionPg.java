/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.pg.tools.squad;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.*;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;

import com.injurytime.storage.api.JpaAccess;

@ActionID(category = "Tools", id = "com.injurytime.tools.squad.ImportSquadJsonAction")
@ActionRegistration(displayName = "#CTL_ImportSquadJsonAction", asynchronous = true)
@ActionReference(path = "Menu/Tools", position = 1300)
@Messages("CTL_ImportSquadJsonAction=Import Squad JSON…")
public final class ImportSquadJsonActionPg implements ActionListener {

    private static final RequestProcessor RP
            = new RequestProcessor(ImportSquadJsonActionPg.class);

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // UI controls
        var seasonField = new JTextField(12);
        var clubField = new JTextField(8);
        var fileField = new JTextField(24);
        fileField.setEditable(false);
        var browse = new JButton("Browse…");

        final File[] chosen = new File[1];
        browse.addActionListener(ae ->
        {
            var fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
            {
                chosen[0] = fc.getSelectedFile();
                fileField.setText(chosen[0].getAbsolutePath());
            }
        });

        // Layout
        var form = new JPanel(new GridBagLayout());
        var gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0;
        gc.gridy = 0;
        form.add(new JLabel("Season ID:"), gc);
        gc.gridx = 1;
        form.add(seasonField, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        form.add(new JLabel("Club API ID:"), gc);
        gc.gridx = 1;
        form.add(clubField, gc);

        gc.gridx = 0;
        gc.gridy = 2;
        form.add(new JLabel("JSON File:"), gc);
        var fileRow = new JPanel(new BorderLayout(6, 0));
        fileRow.add(fileField, BorderLayout.CENTER);
        fileRow.add(browse, BorderLayout.EAST);
        gc.gridx = 1;
        form.add(fileRow, gc);

        var dd = new DialogDescriptor(form, Bundle.CTL_ImportSquadJsonAction());
        Object res = DialogDisplayer.getDefault().notify(dd);
        if (res != NotifyDescriptor.OK_OPTION)
        {
            return;
        }

        // Validate inputs
        String seasonId = seasonField.getText().trim();
        if (seasonId.isEmpty())
        {
            warn("Season ID is required.");
            return;
        }
        int clubApiId;
        try
        {
            clubApiId = Integer.parseInt(clubField.getText().trim());
        } catch (NumberFormatException ex)
        {
            warn("Club API ID must be an integer.");
            return;
        }
        File json = chosen[0];
        if (json == null || !json.isFile())
        {
            warn("Please choose a JSON file.");
            return;
        }

        // Off-EDT work
        RP.post(() ->
        {
            try
            {
                // Lookup JPA access and run importer
                JpaAccess jpa = Lookup.getDefault().lookup(JpaAccess.class);
                if (jpa == null)
                {
                    warn("JPA is not available (JpaAccess service not found).");
                    return;
                }

                var importer = new SquadJsonImporter(jpa);
                var result = importer.importFile(json, seasonId, clubApiId);

                String msg = String.format(
                        "Imported squad for club %d (%s): %d inserted, %d updated, %d skipped",
                        clubApiId, seasonId, result.inserted(), result.updated(), result.skipped()
                );

                var icon = javax.swing.UIManager.getIcon("OptionPane.informationIcon"); // always non-null
                org.openide.awt.NotificationDisplayer.getDefault().notify(
                        "Squad import finished",
                        icon,
                        msg,
                        null
                );

            } catch (Exception ex)
            {
                Exceptions.printStackTrace(ex);
                var icon = javax.swing.UIManager.getIcon("OptionPane.errorIcon");
                NotificationDisplayer.getDefault().notify("Import failed", icon, "See IDE log for details", null);
            }
        });
    }

    private static void warn(String m)
    {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(m, NotifyDescriptor.WARNING_MESSAGE));
    }
}
