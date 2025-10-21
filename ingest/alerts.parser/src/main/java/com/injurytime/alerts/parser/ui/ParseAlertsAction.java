/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.alerts.parser.ui;

import com.injurytime.alerts.parser.ParseAlertsService;   // <- your service class
import com.injurytime.storage.api.JpaAccess;
import org.openide.awt.*;
import org.openide.util.*;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Tools", id = "com.injurytime.alerts.parser.ui.ParseAlertsAction")
@ActionRegistration(displayName = "#CTL_ParseAlertsAction")
@ActionReference(path = "Menu/Tools", position = 1600)
@Messages("CTL_ParseAlertsAction=Parse Alerts → Events")
public final class ParseAlertsAction implements ActionListener {

  private static final RequestProcessor RP =
      new RequestProcessor(ParseAlertsAction.class);

  @Override
  public void actionPerformed(ActionEvent e) {
    RP.post(() -> {
      try {
        // Use a PG-specific JPA access so we definitely hit Postgres
        JpaAccess jpa = new PgJpaAccess();

        final ParseAlertsService.Result[] box = new ParseAlertsService.Result[1];
        BaseProgressUtils.showProgressDialogAndRun(() -> {
          try {
            ParseAlertsService svc = new ParseAlertsService(jpa);
            // parse last 14 days; tweak or make configurable
            box[0] = svc.run(14);
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        }, "Parsing Google Alerts into availability events…");

        ParseAlertsService.Result r = box[0];
        String msg = (r == null)
            ? "Parser ran but returned no result."
            : String.format("Parsed: %d events, rolled up %d, skipped %d, errors %d.",
                r.inserted(), r.updated(), r.skipped(), r.errors());

        DialogDisplayer.getDefault().notify(
            new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE)
        );
      } catch (Throwable t) {
        Exceptions.printStackTrace(t);
      }
    });
  }

  /**
   * Minimal Postgres JpaAccess (private to this module).
   * Requires a PU named 'injurytime-pg' on the classpath.
   */
  static final class PgJpaAccess implements JpaAccess {
    private volatile jakarta.persistence.EntityManagerFactory emf;
    private jakarta.persistence.EntityManagerFactory emf() {
      var ref = emf;
      if (ref == null) {
        synchronized (this) {
          if (emf == null) {
            emf = jakarta.persistence.Persistence.createEntityManagerFactory("injurytime-pg");
          }
          ref = emf;
        }
      }
      return ref;
    }
    @Override
    public <R> R tx(java.util.function.Function<jakarta.persistence.EntityManager,R> work) {
      var em = emf().createEntityManager();
      var tx = em.getTransaction();
      try {
        tx.begin();
        R out = work.apply(em);
        tx.commit();
        return out;
      } catch (RuntimeException ex) {
        if (tx.isActive()) tx.rollback();
        throw ex;
      } finally {
        em.close();
      }
    }
  }
}

