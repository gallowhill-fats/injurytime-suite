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
        preferredID = "FormAllTeamsTopComponent"
)
@ActionReference(path = "Menu/Tools", position = 1820)
@Messages(
        {
            "CTL_FormAllTeams=Form: All Teams (Rolling)"
        })
public final class FixtureDossierTopComponent extends TopComponent {

    private final JTextField txtFixtures = new JTextField(40); // csv fixtureIds
    private final JButton btnLoad = new JButton("Load");
    private final JButton btnPrint = new JButton("Print");

    private final JPanel content = new JPanel(new BorderLayout());
    private final JPanel listPanel = new JPanel();
    private final JScrollPane scroll = new JScrollPane(listPanel);

    public FixtureDossierTopComponent()
    {
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

        scroll.setWheelScrollingEnabled(true);
        scroll.getVerticalScrollBar().setUnitIncrement(24);

        scroll.setWheelScrollingEnabled(true);
        scroll.getVerticalScrollBar().setUnitIncrement(24);   // smoother speed
        scroll.getHorizontalScrollBar().setUnitIncrement(24);

        btnLoad.addActionListener(e -> doLoad());
        btnPrint.addActionListener(e -> PrintUtil.printComponent(listPanel));

        listPanel.addMouseWheelListener(e ->
        {
            var vbar = scroll.getVerticalScrollBar();
            int units = (e.getScrollType() == java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL)
                    ? e.getUnitsToScroll()
                    : e.getWheelRotation() * 3; // block scroll fallback
            vbar.setValue(vbar.getValue() + vbar.getUnitIncrement() * units);
        });
    }

    private void doLoad()
    {
        String csv = txtFixtures.getText().trim();
        if (csv.isEmpty())
        {
            return;
        }
        List<Integer> fids = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();

        listPanel.removeAll();
        listPanel.add(new JLabel("Loading..."));
        listPanel.revalidate();
        listPanel.repaint();

        RequestProcessor.getDefault().post(() ->
        {
            var svc = Lookup.getDefault().lookup(DossierService.class);
            if (svc == null)
            {
                SwingUtilities.invokeLater(() ->
                {
                    listPanel.removeAll();
                    listPanel.add(new JLabel("No DossierService available."));
                    listPanel.revalidate();
                    listPanel.repaint();
                });
                return;
            }
            Map<Integer, FixtureDossier> data = svc.loadDossiers(fids);

            SwingUtilities.invokeLater(() ->
            {
                listPanel.removeAll();
                if (data.isEmpty())
                {
                    listPanel.add(new JLabel("No data."));
                } else
                {
                    data.values().forEach(d -> listPanel.add(buildFixtureSection(d)));
                }
                listPanel.revalidate();
                listPanel.repaint();
            });
        });
    }

    private JComponent buildFixtureSection(FixtureDossier d)
    {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 8, 8, 8),
                BorderFactory.createTitledBorder(
                        "%s vs %s  (Fixture %d, League %d / %d)".formatted(d.homeName(), d.awayName(), d.fixtureId(), d.leagueId(), d.season()))
        ));

        Accordion acc = new Accordion();

        // 1) Team Stats table (home | away side-by-side)
        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 8, 8));
        statsPanel.add(tableForStats("Home: " + d.homeName(), d.teamStatsHome()));
        statsPanel.add(tableForStats("Away: " + d.awayName(), d.teamStatsAway()));
        acc.addSection("Team stats", wrap(statsPanel));

        // 2) Last-5 3-1-0 plot (x = games in chronological order; no backtracking)
        var yH = valuesOldToNew(d.last5Home()); // oldest → newest
        var yA = valuesOldToNew(d.last5Away());

        var xH = oneToN(yH.size());             // 1..N
        var xA = oneToN(yA.size());

        acc.addSection("Last 5 (3-1-0, games ago)",
                XCharts.line("Form (last 5)", "Games (old → new)", "Points",
                        xH, yH, d.homeName(),
                        xA, yA, d.awayName()
                )
        );

        // 3) Histograms (home / away / league)
        // 3) Score histograms (stacked by venue)
        var homeTeam = buildStackedFromHomeAway(
                d.homeHistAtHome(), // when the dossier's HOME team played at HOME
                d.homeHistAway(), // when the dossier's HOME team played AWAY (labels are team-perspective)
                /*trimSparse*/ false, /*minCount*/ 0, /*keepTopMass*/ 1.0 // no trimming while validating
        );

        var awayTeam = buildStackedFromHomeAway(
                d.awayHistAtHome(), // when the dossier's AWAY team played at HOME
                d.awayHistAway(), // when the dossier's AWAY team played AWAY (team-perspective)
                /*trimSparse*/ false, /*minCount*/ 0, /*keepTopMass*/ 1.0
        );

        var league = buildStackedFromHomeAway(
                d.leagueHistHome(), // league home-perspective bins
                d.leagueHistAway(), // league away-perspective bins (gf/ga already swapped in service)
                /*trimSparse*/ false, /*minCount*/ 0, /*keepTopMass*/ 1.0
        );

// Optional sanity totals — area under bars should equal matches counted
        int totHome = homeTeam.home.stream().mapToInt(Integer::intValue).sum() + homeTeam.away.stream().mapToInt(Integer::intValue).sum();
        int totAway = awayTeam.home.stream().mapToInt(Integer::intValue).sum() + awayTeam.away.stream().mapToInt(Integer::intValue).sum();
        int totLeague = league.home.stream().mapToInt(Integer::intValue).sum() + league.away.stream().mapToInt(Integer::intValue).sum();

        JComponent homeBars = XCharts.barsHomeAwayStacked(
                "Home team: " + d.homeName() + " (" + totHome + " games)",
                homeTeam.labels, homeTeam.home, homeTeam.away
        );
        JComponent awayBars = XCharts.barsHomeAwayStacked(
                "Away team: " + d.awayName() + " (" + totAway + " games)",
                awayTeam.labels, awayTeam.home, awayTeam.away
        );
        JComponent leagueBars = XCharts.barsHomeAwayStacked(
                "League scores (" + totLeague + " games)",
                league.labels, league.home, league.away
        );

        acc.addSection("Score histograms", wrap3(homeBars, awayBars, leagueBars));

        var teamStack = buildStackedFromHomeAway(
                d.histHome(), // counts per team-perspective score when playing at HOME
                d.histAway(), // counts per team-perspective score when playing AWAY
                /*trimSparse*/ true, /*minCount*/ 1, /*keepTopMass*/ 0.99
        );

        JComponent teamBars = XCharts.barsHomeAwayStacked(
                "Score Distribution (" + d.homeName() + " vs " + d.awayName() + ")",
                teamStack.labels, teamStack.home, teamStack.away
        );
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

    private static JPanel wrap(JComponent c)
    {
        JPanel p = new JPanel(new BorderLayout());
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private static JPanel wrap2(JComponent a, JComponent b)
    {
        JPanel p = new JPanel(new GridLayout(1, 2, 8, 8));
        p.add(a);
        p.add(b);
        return p;
    }

    private static JPanel wrap3(JComponent a, JComponent b, JComponent c)
    {
        JPanel p = new JPanel(new GridLayout(1, 3, 8, 8));
        p.add(a);
        p.add(b);
        p.add(c);
        return p;
    }

    private JComponent scrollText(String s, String title) {
  JTextArea ta = new JTextArea(s);
  ta.setEditable(false);
  ta.setLineWrap(true);
  ta.setWrapStyleWord(true);
  JScrollPane sp = new JScrollPane(ta);
  sp.setBorder(BorderFactory.createTitledBorder(title));
  forwardWheelToOuter(sp);
  return sp;
}

private JTable tableForStats(String title, List<StatRow> rows) {
  String[] cols = {"stat", "count", "%", "per-game"};
  DefaultTableModel m = new DefaultTableModel(cols, 0) {
    @Override public boolean isCellEditable(int r, int c) { return false; }
  };
  for (StatRow r : rows) {
    m.addRow(new Object[]{r.code(), r.count(), fmt(r.pct()), fmt(r.perGame())});
  }
  JTable t = new JTable(m);
  t.setAutoCreateRowSorter(true);
  JScrollPane sp = new JScrollPane(t);
  sp.setBorder(BorderFactory.createTitledBorder(title));
  forwardWheelToOuter(sp);
  JPanel wrap = new JPanel(new BorderLayout());
  wrap.add(sp, BorderLayout.CENTER);
  return t; // (keep your existing return type if you rely on it) 
}

private JTable tableUnavailable(String title, List<Unavailable> rows) {
  String[] cols = {"playerId", "name", "type", "status", "notes"};
  DefaultTableModel m = new DefaultTableModel(cols, 0) {
    @Override public boolean isCellEditable(int r, int c) { return false; }
  };
  for (Unavailable u : rows) {
    m.addRow(new Object[]{u.playerId(), u.playerName(), u.type(), u.status(), u.notes()});
  }
  JTable t = new JTable(m);
  t.setAutoCreateRowSorter(true);
  JScrollPane sp = new JScrollPane(t);
  sp.setBorder(BorderFactory.createTitledBorder(title));
  forwardWheelToOuter(sp);
  JPanel wrap = new JPanel(new BorderLayout());
  wrap.add(sp, BorderLayout.CENTER);
  return t;
}

private JTable tableScorers(String title, List<Scorer> rows) {
  String[] cols = {"playerId", "name", "goals"};
  DefaultTableModel m = new DefaultTableModel(cols, 0) {
    @Override public boolean isCellEditable(int r, int c) { return false; }
  };
  for (Scorer s : rows) {
    m.addRow(new Object[]{s.playerId(), s.playerName(), s.goals()});
  }
  JTable t = new JTable(m);
  t.setAutoCreateRowSorter(true);
  JScrollPane sp = new JScrollPane(t);
  sp.setBorder(BorderFactory.createTitledBorder(title));
  forwardWheelToOuter(sp);
  JPanel wrap = new JPanel(new BorderLayout());
  wrap.add(sp, BorderLayout.CENTER);
  return t;
}



    private static String fmt(Double d)
    {
        return d == null ? "" : String.format(Locale.US, "%.2f", d);
    }

    private static List<String> labels(List<ScoreBin> bins)
    {
        return bins.stream().map(ScoreBin::label).toList();
    }

    private static List<Integer> counts(List<ScoreBin> bins)
    {
        return bins.stream().map(ScoreBin::count).toList();
    }

    private static String joinLines(List<MatchNote> notes)
    {
        if (notes.isEmpty())
        {
            return "(no recent details)";
        }
        return notes.stream().map(MatchNote::line).collect(Collectors.joining("\n"));
    }

    // inside FixtureDossierTopComponent { ... }
    private static final class StackedData {

        final java.util.List<String> labels;
        final java.util.List<Integer> home;
        final java.util.List<Integer> away;

        StackedData(java.util.List<String> labels, java.util.List<Integer> home, java.util.List<Integer> away)
        {
            this.labels = labels;
            this.home = home;
            this.away = away;
        }
    }

    private static StackedData buildStackedFromHomeAway(
            java.util.List<ScoreBin> homeBins,
            java.util.List<ScoreBin> awayBins,
            boolean trimSparse,
            int minCount,
            double keepTopMass)
    {

        final String OTHER = "other";

        // 1) collect totals per label
        java.util.Map<String, int[]> map = new java.util.HashMap<>();
        for (ScoreBin b : homeBins)
        {
            map.computeIfAbsent(b.label(), k -> new int[2])[0] += b.count();
        }
        for (ScoreBin b : awayBins)
        {
            map.computeIfAbsent(b.label(), k -> new int[2])[1] += b.count();
        }

        // 2) Split entries into numeric labels vs "other"/non-parsable, merging any non-parsable into OTHER
        java.util.Map<String, int[]> numeric = new java.util.HashMap<>();
        int[] otherCounts = map.containsKey(OTHER) ? map.get(OTHER) : new int[2];

        java.util.function.Function<String, int[]> tryParse = lab ->
        {
            // accept "a-b" (hyphen) or "a–b" (en-dash)
            int sep = lab.indexOf('–');
            if (sep < 0)
            {
                sep = lab.indexOf('-');
            }
            if (sep <= 0 || sep >= lab.length() - 1)
            {
                return null;
            }
            try
            {
                int gf = Integer.parseInt(lab.substring(0, sep).trim());
                int ga = Integer.parseInt(lab.substring(sep + 1).trim());
                return new int[]
                {
                    gf, ga
                };
            } catch (NumberFormatException ex)
            {
                return null;
            }
        };

        for (var e : map.entrySet())
        {
            String lab = e.getKey();
            int[] c = e.getValue();
            if (OTHER.equalsIgnoreCase(lab))
            {
                otherCounts[0] += c[0];
                otherCounts[1] += c[1];
                continue;
            }
            int[] parsed = tryParse.apply(lab);
            if (parsed == null)
            {
                otherCounts[0] += c[0];
                otherCounts[1] += c[1];
            } else
            {
                numeric.merge(lab, c, (a, b) -> new int[]
                {
                    a[0] + b[0], a[1] + b[1]
                });
            }
        }

        // 3) gather entries and (optionally) trim sparse tail (includes OTHER)
        java.util.List<java.util.Map.Entry<String, int[]>> entries = new java.util.ArrayList<>(numeric.entrySet());
        if (otherCounts[0] + otherCounts[1] > 0)
        {
            entries.add(new java.util.AbstractMap.SimpleEntry<>(OTHER, otherCounts));
        }

        if (trimSparse)
        {
            entries.removeIf(e -> (e.getValue()[0] + e.getValue()[1]) < minCount);
            int total = entries.stream().mapToInt(e -> e.getValue()[0] + e.getValue()[1]).sum();
            if (total > 0 && keepTopMass < 0.9999)
            {
                // rank by total count (desc) and keep until reaching keepTopMass
                var byFreq = new java.util.ArrayList<>(entries);
                byFreq.sort(java.util.Comparator.<java.util.Map.Entry<String, int[]>>comparingInt(e -> e.getValue()[0] + e.getValue()[1]).reversed());
                int cum = 0;
                var keep = new java.util.HashSet<String>();
                for (var e : byFreq)
                {
                    keep.add(e.getKey());
                    cum += e.getValue()[0] + e.getValue()[1];
                    if (cum >= Math.ceil(total * keepTopMass))
                    {
                        break;
                    }
                }
                entries.removeIf(e -> !keep.contains(e.getKey()));
            }
        }

        // 4) sort: numeric labels by (gf,ga), "other" goes last
        java.util.function.Function<String, int[]> parseForSort = lab ->
        {
            int[] p = tryParse.apply(lab);
            return (p != null) ? p : new int[]
            {
                Integer.MAX_VALUE, Integer.MAX_VALUE
            };
        };

        entries.sort((a, b) ->
        {
            boolean aOther = OTHER.equalsIgnoreCase(a.getKey());
            boolean bOther = OTHER.equalsIgnoreCase(b.getKey());
            if (aOther && bOther)
            {
                return 0;
            }
            if (aOther)
            {
                return 1;  // a after b
            }
            if (bOther)
            {
                return -1; // b after a
            }
            int[] A = parseForSort.apply(a.getKey()), B = parseForSort.apply(b.getKey());
            int cmp = Integer.compare(A[0], B[0]);
            return (cmp != 0) ? cmp : Integer.compare(A[1], B[1]);
        });

        // 5) emit aligned lists
        var labels = new java.util.ArrayList<String>(entries.size());
        var home = new java.util.ArrayList<Integer>(entries.size());
        var away = new java.util.ArrayList<Integer>(entries.size());
        for (var e : entries)
        {
            labels.add(e.getKey());
            home.add(e.getValue()[0]);
            away.add(e.getValue()[1]);
        }
        return new StackedData(labels, home, away);
    }

    private static java.util.List<Double> valuesOldToNew(java.util.List<DossierService.WeekPoint> pts)
    {
        if (pts == null || pts.isEmpty())
        {
            return java.util.List.of();
        }
        var copy = new java.util.ArrayList<DossierService.WeekPoint>(pts);
        java.util.Collections.reverse(copy); // assume incoming is newest→oldest
        var out = new java.util.ArrayList<Double>(copy.size());
        for (var p : copy)
        {
            out.add(p.value());
        }
        return out; // oldest → newest
    }

    private static java.util.List<Integer> oneToN(int n)
    {
        var xs = new java.util.ArrayList<Integer>(n);
        for (int i = 1; i <= n; i++)
        {
            xs.add(i);
        }
        return xs; // 1..N strictly increasing
    }

//    private void forwardWheelToOuter(JScrollPane inner)
//    {
//        inner.setWheelScrollingEnabled(false); // let outer scroll own the wheel
//        inner.addMouseWheelListener(e ->
//        {
//            var vbar = scroll.getVerticalScrollBar();
//            int units = (e.getScrollType() == java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL)
//                    ? e.getUnitsToScroll()
//                    : e.getWheelRotation() * 3;
//            vbar.setValue(vbar.getValue() + vbar.getUnitIncrement() * units);
//            e.consume();
//        });
//    }
    
    private void forwardWheelToOuter(JScrollPane inner) {
  inner.setWheelScrollingEnabled(false);  // outer scroll owns the wheel
  inner.addMouseWheelListener(e -> {
    var vbar = scroll.getVerticalScrollBar();
    int units = (e.getScrollType() == java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL)
        ? e.getUnitsToScroll()
        : e.getWheelRotation() * 3;
    vbar.setValue(vbar.getValue() + vbar.getUnitIncrement() * units);
    e.consume();
  });
}


}
