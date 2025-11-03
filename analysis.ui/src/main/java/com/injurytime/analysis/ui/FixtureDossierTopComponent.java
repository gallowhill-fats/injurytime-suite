/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.ui;

import com.injurytime.analysis.api.DossierService;
import com.injurytime.analysis.api.DossierService.*;

import org.openide.util.Lookup;
import org.openide.windows.TopComponent;
import org.openide.util.RequestProcessor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;

@TopComponent.Description(
    preferredID = "FormAllTeamsTopComponent",
    persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "com.injurytime.analysis.ui.FormAllTeamsTopComponent")
@TopComponent.OpenActionRegistration(
    displayName = "#CTL_FormAllTeams",
    preferredID  = "FormAllTeamsTopComponent"
)
@ActionReference(path = "Menu/Tools", position = 1820)
@Messages({
    "CTL_FormAllTeams=Form: All Teams (Rolling)"
})
public final class FixtureDossierTopComponent extends TopComponent {

  private final JTextField txtFixtures = new JTextField(40); // csv fixtureIds
  private final JButton btnLoad = new JButton("Load");
  private final JButton btnPrint = new JButton("Print");

  private final JPanel content = new JPanel(new BorderLayout());
  private final JPanel listPanel = new JPanel();
  private final JScrollPane scroll = new JScrollPane(listPanel);

  public FixtureDossierTopComponent() {
    setName("Fixture Dossier");
    setLayout(new BorderLayout());

    JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
    north.add(new JLabel("Fixture IDs (CSV): "));
    north.add(txtFixtures);
    north.add(btnLoad);
    north.add(btnPrint);
    add(north, BorderLayout.NORTH);

    listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
    add(scroll, BorderLayout.CENTER);

    btnLoad.addActionListener(e -> doLoad());
    btnPrint.addActionListener(e -> PrintUtil.printComponent(listPanel));
  }

  private void doLoad() {
    String csv = txtFixtures.getText().trim();
    if (csv.isEmpty()) return;
    List<Integer> fids = Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(Integer::parseInt)
        .toList();

    listPanel.removeAll();
    listPanel.add(new JLabel("Loading..."));
    listPanel.revalidate();
    listPanel.repaint();

    RequestProcessor.getDefault().post(() -> {
      var svc = Lookup.getDefault().lookup(DossierService.class);
      if (svc == null) {
        SwingUtilities.invokeLater(() -> {
          listPanel.removeAll();
          listPanel.add(new JLabel("No DossierService available."));
          listPanel.revalidate(); listPanel.repaint();
        });
        return;
      }
      Map<Integer, FixtureDossier> data = svc.loadDossiers(fids);

      SwingUtilities.invokeLater(() -> {
        listPanel.removeAll();
        if (data.isEmpty()) {
          listPanel.add(new JLabel("No data."));
        } else {
          data.values().forEach(d -> listPanel.add(buildFixtureSection(d)));
        }
        listPanel.revalidate(); listPanel.repaint();
      });
    });
  }

  private JComponent buildFixtureSection(FixtureDossier d) {
    JPanel outer = new JPanel(new BorderLayout());
    outer.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(8,8,8,8),
        BorderFactory.createTitledBorder(
            "%s vs %s  (Fixture %d, League %d / %d)".formatted(d.homeName(), d.awayName(), d.fixtureId(), d.leagueId(), d.season()))
    ));

    Accordion acc = new Accordion();

    // 1) Team Stats table (home | away side-by-side)
    JPanel statsPanel = new JPanel(new GridLayout(1, 2, 8, 8));
    statsPanel.add(tableForStats("Home: " + d.homeName(), d.teamStatsHome()));
    statsPanel.add(tableForStats("Away: " + d.awayName(), d.teamStatsAway()));
    acc.addSection("Team stats", wrap(statsPanel));

    // 2) Last-5 3-1-0 plot
    var xH = d.last5Home().stream().map(WeekPoint::week).collect(Collectors.toList());
    var yH = d.last5Home().stream().map(WeekPoint::value).collect(Collectors.toList());
    var xA = d.last5Away().stream().map(WeekPoint::week).collect(Collectors.toList());
    var yA = d.last5Away().stream().map(WeekPoint::value).collect(Collectors.toList());
    acc.addSection("Last 5 (3-1-0, by week)", XCharts.line("Form (last 5)", "Week", "Points",
        xH, yH, d.homeName(), xA, yA, d.awayName()));

    // 3) Histograms (home / away / league)
    acc.addSection("Score histograms",
        wrap3(XCharts.bars("Home scores (" + d.homeName() + ")", labels(d.histHome()), counts(d.histHome())),
              XCharts.bars("Away scores (" + d.awayName() + ")", labels(d.histAway()), counts(d.histAway())),
              XCharts.bars("League scores", labels(d.histLeague()), counts(d.histLeague()))));

    // 4) Last-5 summaries
    acc.addSection("Last-5 summaries",
        wrap2(scrollText(joinLines(d.last5NotesHome()), "Home"),
              scrollText(joinLines(d.last5NotesAway()), "Away")));

    // 5) Unavailability
    acc.addSection("Unavailability",
        wrap2(tableUnavailable("Home", d.unavailableHome()),
              tableUnavailable("Away", d.unavailableAway())));

    // 6) Top scorers
    acc.addSection("Top scorers",
        wrap2(tableScorers("Home", d.topScorersHome()),
              tableScorers("Away", d.topScorersAway())));

    outer.add(acc, BorderLayout.CENTER);
    return outer;
  }

  private static JPanel wrap(JComponent c) {
    JPanel p = new JPanel(new BorderLayout());
    p.add(c, BorderLayout.CENTER);
    return p;
  }
  private static JPanel wrap2(JComponent a, JComponent b) {
    JPanel p = new JPanel(new GridLayout(1,2,8,8));
    p.add(a); p.add(b);
    return p;
  }
  private static JPanel wrap3(JComponent a, JComponent b, JComponent c) {
    JPanel p = new JPanel(new GridLayout(1,3,8,8));
    p.add(a); p.add(b); p.add(c);
    return p;
  }

  private static JComponent scrollText(String s, String title) {
    JTextArea ta = new JTextArea(s);
    ta.setEditable(false);
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    JScrollPane sp = new JScrollPane(ta);
    sp.setBorder(BorderFactory.createTitledBorder(title));
    return sp;
  }

  private static JTable tableForStats(String title, List<StatRow> rows) {
    String[] cols = {"stat", "count", "%", "per-game"};
    DefaultTableModel m = new DefaultTableModel(cols, 0) {
      @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    for (StatRow r : rows) {
      m.addRow(new Object[]{r.code(), r.count(), fmt(r.pct()), fmt(r.perGame())});
    }
    JTable t = new JTable(m);
    t.setAutoCreateRowSorter(true);
    JPanel wrap = new JPanel(new BorderLayout());
    wrap.setBorder(BorderFactory.createTitledBorder(title));
    wrap.add(new JScrollPane(t), BorderLayout.CENTER);
    return t;
  }

  private static JTable tableUnavailable(String title, List<Unavailable> rows) {
    String[] cols = {"playerId", "name", "type", "status", "notes"};
    DefaultTableModel m = new DefaultTableModel(cols, 0) {
      @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    for (Unavailable u : rows) {
      m.addRow(new Object[]{u.playerId(), u.playerName(), u.type(), u.status(), u.notes()});
    }
    JTable t = new JTable(m);
    t.setAutoCreateRowSorter(true);
    JPanel wrap = new JPanel(new BorderLayout());
    wrap.setBorder(BorderFactory.createTitledBorder(title));
    wrap.add(new JScrollPane(t), BorderLayout.CENTER);
    return t;
  }

  private static JTable tableScorers(String title, List<Scorer> rows) {
    String[] cols = {"playerId", "name", "goals"};
    DefaultTableModel m = new DefaultTableModel(cols, 0) {
      @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    for (Scorer s : rows) {
      m.addRow(new Object[]{s.playerId(), s.playerName(), s.goals()});
    }
    JTable t = new JTable(m);
    t.setAutoCreateRowSorter(true);
    JPanel wrap = new JPanel(new BorderLayout());
    wrap.setBorder(BorderFactory.createTitledBorder(title));
    wrap.add(new JScrollPane(t), BorderLayout.CENTER);
    return t;
  }

  private static String fmt(Double d) { return d == null ? "" : String.format(Locale.US, "%.2f", d); }
  private static List<String> labels(List<ScoreBin> bins) { return bins.stream().map(ScoreBin::label).toList(); }
  private static List<Integer> counts(List<ScoreBin> bins) { return bins.stream().map(ScoreBin::count).toList(); }
  private static String joinLines(List<MatchNote> notes) {
    if (notes.isEmpty()) return "(no recent details)";
    return notes.stream().map(MatchNote::line).collect(Collectors.joining("\n"));
  }
}

