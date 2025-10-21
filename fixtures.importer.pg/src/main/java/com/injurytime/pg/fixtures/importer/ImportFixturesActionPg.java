/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.pg.fixtures.importer;

import com.injurytime.storage.api.JpaAccess;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;

@ActionID(category = "Tools", id = "com.injurytime.pg.fixtures.importer.ImportFixturesActionPg")
@ActionRegistration(displayName = "#CTL_ImportFixturesActionPg")
@ActionReference(path = "Menu/Tools", position = 1450)
@Messages("CTL_ImportFixturesActionPg=Import Fixtures (JSON → PG)…")
public final class ImportFixturesActionPg implements ActionListener {

    private static final Logger LOG = Logger.getLogger(ImportFixturesActionPg.class.getName());
    private final JpaAccess jpa = Lookup.getDefault().lookup(JpaAccess.class);

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (jpa == null)
        {
            LOG.severe("JpaAccess not found (check storage.impl.jpa module load)");
            return;
        }

        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
        if (ch.showOpenDialog(WindowManager.getDefault().getMainWindow()) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }

        File json = ch.getSelectedFile();
        try
        {
            String content = Files.readString(json.toPath());
            FixturesJsonImporterPg.Result res = new FixturesJsonImporterPg(jpa).importJson(content);

            String msg = """
      Fixtures processed: %d
      Inserted: %d
      Updated:  %d
      """.formatted(res.total(), res.inserted(), res.updated());

            DialogDisplayer.getDefault().notify(
                    new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE)
            );
        } catch (Exception ex)
        {
            DialogDisplayer.getDefault().notify(
                    new NotifyDescriptor.Message("Import failed: " + ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE)
            );
        }

    }
}
