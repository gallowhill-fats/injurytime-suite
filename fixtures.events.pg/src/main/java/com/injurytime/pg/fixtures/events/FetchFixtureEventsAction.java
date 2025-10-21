/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.pg.fixtures.events;

import com.injurytime.storage.api.JpaAccess;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.openide.awt.*;
import org.openide.util.*;
import org.openide.util.NbBundle.Messages;

@ActionID(category="Tools", id="com.injurytime.pg.fixtures.events.FetchFixtureEventsAction")
@ActionRegistration(displayName="#CTL_FetchFixtureEventsAction")
@ActionReference(path="Menu/Tools", position=1550)
@Messages("CTL_FetchFixtureEventsAction=Fetch Fixture Events (API → PG)…")
public final class FetchFixtureEventsAction implements ActionListener {
  private static final Logger LOG = Logger.getLogger(FetchFixtureEventsAction.class.getName());
  private final JpaAccess jpa = Lookup.getDefault().lookup(JpaAccess.class);

  @Override public void actionPerformed(ActionEvent e) {
    if (jpa == null) { JOptionPane.showMessageDialog(null, "JpaAccess missing"); return; }

    // Quick prompt (you can replace with a proper dialog)
    String leagueStr = JOptionPane.showInputDialog(null, "League ID (e.g. 39 EPL, 40 CHAMP):", "40");
    String seasonStr = JOptionPane.showInputDialog(null, "Season (e.g. 2024):", "2024");
    if (leagueStr == null || seasonStr == null) return;

    int leagueId = Integer.parseInt(leagueStr.trim());
    int season   = Integer.parseInt(seasonStr.trim());

    EventsFetcher fetcher = new EventsFetcher(jpa);
    EventsFetcher.Result res = fetcher.fetchAndUpsertForSeason(leagueId, season);

    JOptionPane.showMessageDialog(
      null,
      ("Fixtures scanned: %d\nCalls made: %d\nEvents inserted: %d\nEvents updated: %d\nRaw rows upserted: %d\nErrors: %d")
        .formatted(res.scanned(), res.calls(), res.inserted(), res.updated(), res.rawUpserts(), res.errors())
    );
  }
}

