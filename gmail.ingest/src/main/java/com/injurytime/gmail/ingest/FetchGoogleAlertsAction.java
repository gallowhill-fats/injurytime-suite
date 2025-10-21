/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.gmail.ingest;

import com.injurytime.storage.api.JpaAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Tools", id = "com.injurytime.gmail.ingest.FetchGoogleAlertsAction")
@ActionRegistration(displayName = "#CTL_FetchGoogleAlerts")
@ActionReference(path = "Menu/Tools", position = 1475)
@Messages("CTL_FetchGoogleAlerts=Fetch Google Alerts (Gmail)…")
public final class FetchGoogleAlertsAction implements ActionListener {

  private static final RequestProcessor RP = new RequestProcessor(FetchGoogleAlertsAction.class);

  @Override
  public void actionPerformed(ActionEvent e) {
    RP.post(() -> {
      try {
//        JpaAccess jpa = Lookup.getDefault().lookup(JpaAccess.class);

           JpaAccess jpa = new com.injurytime.gmail.ingest.PgJpaAccess();   // <-- force Postgres
           GoogleAlertsIngest ingest = new GoogleAlertsIngest(jpa);

        final GoogleAlertsIngest.Result[] box = new GoogleAlertsIngest.Result[1];

        BaseProgressUtils.showProgressDialogAndRun(() -> {
          try {
            box[0] = ingest.fetchAndStore(50);
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        }, "Fetching Google Alerts (Gmail)…");

        GoogleAlertsIngest.Result res = box[0] == null ? new GoogleAlertsIngest.Result(0,0,1) : box[0];
        String msg = String.format("Google Alerts: %d inserted, %d skipped, %d errors.",
            res.inserted(), res.skipped(), res.errors());
        DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(
            msg, NotifyDescriptor.INFORMATION_MESSAGE));

      } catch (Throwable t) {
        Exceptions.printStackTrace(t);
      }
    });
  }
}

