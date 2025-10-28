/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.ui;

import com.injurytime.analysis.api.FormService;
import com.injurytime.analysis.api.WeekPoint;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

@TopComponent.Description(preferredID = "FormAllTeamsTopComponent",
                          persistenceType = TopComponent.PERSISTENCE_NEVER)
@TopComponent.Registration(mode = "editor", openAtStartup = false)

// Create an “Open …” action for this TopComponent:
@ActionID(category = "Window",
          id = "com.injurytime.analysis.ui.FormAllTeamsTopComponent")
@TopComponent.OpenActionRegistration(displayName = "#CTL_ShowFormAllTeams")

// Put the action into the Windows menu:
@ActionReference(path = "Menu/Window", position = 1820)

@Messages({
  "CTL_ShowFormAllTeams=Form: All Teams (Rolling)",
  "TTL_FormAllTeams=Rolling Form (All Teams)"
})
public final class FormAllTeamsTopComponent extends TopComponent {

  private final FormChartPanel chart;
  
  private final java.util.List<JCheckBox> teamChecks = new java.util.ArrayList<>();
  private final java.util.Map<String, JCheckBox> checkByName = new java.util.HashMap<>();

  private static final int LEAGUE_ID = 43;
  private static final int SEASON    = 2025;
  private static final int WINDOW_N  = 5;

  // UI bits
  private final JPanel teamsPanel = new JPanel();
  private final JButton btnAll = new JButton("All");
  private final JButton btnNone = new JButton("None");

  // Data we keep so we can toggle visibility without recomputing
  private Map<Integer, List<WeekPoint>> seriesMap = Map.of();
  private Map<Integer, String> labels = Map.of();
  // quick index from team name to teamId (for convenience)
  private final Map<String, Integer> nameToId = new HashMap<>();
  
  // prefs keys
private static final String PREF_LEAGUE = "form.leagueId";
private static final String PREF_SEASON = "form.season";
private static final String PREF_WINDOW = "form.window";

private final JTextField txtLeague = new JTextField(5);
private final JTextField txtSeason = new JTextField(5);
private final JSpinner  spWindow   = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
private final JButton   btnLoad    = new JButton("Load");


  public FormAllTeamsTopComponent() {
    setName(Bundle.TTL_FormAllTeams());
    setLayout(new BorderLayout());
    
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
  bar.add(new JLabel("League:"));
  bar.add(txtLeague);
  bar.add(new JLabel("Season:"));
  bar.add(txtSeason);
  bar.add(new JLabel("Window:"));
  bar.add(spWindow);
  bar.add(btnLoad);
  add(bar, BorderLayout.NORTH);
  
  btnLoad.addActionListener(evt -> {
    Integer league = parseIntOrNull(txtLeague.getText());
    Integer season = parseIntOrNull(txtSeason.getText());
    int window = (int) spWindow.getValue();
    if (league == null || season == null) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    var p = NbPreferences.forModule(FormAllTeamsTopComponent.class);
    p.putInt(PREF_LEAGUE, league);
    p.putInt(PREF_SEASON, season);
    p.putInt(PREF_WINDOW, window);

    // refresh
    loadAndRender(league, season, window);
  });
  
  //Chart (right)
    chart = new FormChartPanel("Rolling Form (Window " + WINDOW_N + "), League " + LEAGUE_ID + " / " + SEASON);
    add(chart, BorderLayout.CENTER); 

  }
          
          private static Integer parseIntOrNull(String s) {
  try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
}
  
  private JPanel buildSidePanel(Map<Integer,String> labels) {
  // make sure global lists are fresh
  teamChecks.clear();
  checkByName.clear();

  JPanel side = new JPanel(new BorderLayout());

  JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
  JButton showAllBtn  = new JButton("Show all");
  JButton clearAllBtn = new JButton("Clear");
  buttons.add(showAllBtn);
  buttons.add(clearAllBtn);
  side.add(buttons, BorderLayout.NORTH);

  JPanel list = new JPanel();
  list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

  labels.entrySet().stream()
    .sorted(java.util.Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
    .forEach(e -> {
      String teamName = e.getValue();

      // ✅ start checked so it matches the initially drawn series
      JCheckBox cb = new JCheckBox(teamName, true);

      cb.addItemListener(ev -> {
        boolean visible = (ev.getStateChange() == ItemEvent.SELECTED);
        SwingUtilities.invokeLater(() -> chart.setSeriesVisible(teamName, visible));
      });

      teamChecks.add(cb);
      checkByName.put(teamName, cb);
      list.add(cb);
    });

  side.add(new JScrollPane(list,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
      BorderLayout.CENTER);

  showAllBtn.addActionListener(e ->
      SwingUtilities.invokeLater(() -> {
        for (JCheckBox cb : teamChecks) cb.setSelected(true);  // fires listeners -> show
      })
  );
  clearAllBtn.addActionListener(e ->
      SwingUtilities.invokeLater(() -> {
        for (JCheckBox cb : teamChecks) cb.setSelected(false); // fires listeners -> hide
      })
  );

  return side;
}



  @Override
protected void componentOpened() {
  // read defaults from prefs (fallbacks shown)
  var p = NbPreferences.forModule(FormAllTeamsTopComponent.class);
  int league = p.getInt(PREF_LEAGUE, 39);   // default EPL-ish
  int season = p.getInt(PREF_SEASON, 2025);
  int window = p.getInt(PREF_WINDOW, 5);

  // show current values in the UI
  txtLeague.setText(String.valueOf(league));
  txtSeason.setText(String.valueOf(season));
  spWindow.setValue(window);

  // first load
  loadAndRender(league, season, window);
}



  private void autoSelectSome(int n) {
    int count = 0;
    for (Component c : teamsPanel.getComponents()) {
      if (c instanceof JCheckBox cb) {
        cb.setSelected(count < n);
        toggleSeries(cb.getText(), true);
        count++;
        if (count >= n) break;
      }
    }
    chart.repaintChart();
  }

  private void setAllCheckboxes(boolean selected) {
    for (Component c : teamsPanel.getComponents()) {
      if (c instanceof JCheckBox cb) {
        if (cb.isSelected() != selected) {
          cb.setSelected(selected);
          toggleSeries(cb.getText(), selected);
        }
      }
    }
    chart.repaintChart();
  }

  /** Add/remove series for a team name based on checkbox. */
  private void toggleSeries(String teamName, boolean show) {
    Integer teamId = nameToId.get(teamName);
    if (teamId == null) return;

    if (!show) {
      chart.removeSeries(teamName);
      return;
    }

    List<WeekPoint> pts = seriesMap.get(teamId);
    if (pts == null || pts.isEmpty()) return;

    List<Integer> weeks = pts.stream().map(WeekPoint::week).toList();
    List<Double>  vals  = pts.stream().map(WeekPoint::value).toList();

    chart.addSeries(teamName, weeks, vals);
  }
  
  private JCheckBox makeTeamCheckbox(String teamName, boolean initialSelected) {
  JCheckBox cb = new JCheckBox(teamName, initialSelected);
  cb.addItemListener(ev -> {
  boolean visible = (ev.getStateChange() == ItemEvent.SELECTED);
  SwingUtilities.invokeLater(() -> chart.setSeriesVisible(teamName, visible));
});
  return cb;
}
  
  private void loadAndRender(int leagueId, int season, int windowN) {
  // compute off-EDT
  RequestProcessor.getDefault().post(() -> {
    FormService svc = Lookup.getDefault().lookup(FormService.class);
    if (svc == null) return;

    Map<Integer, List<WeekPoint>> sm  = svc.leagueRollingForm(leagueId, season, windowN);
    Map<Integer, String>          lbl = svc.teamLabels(leagueId, season);

    SwingUtilities.invokeLater(() -> {
  // reset per-league state
  this.seriesMap = sm;
  this.labels    = lbl;

  teamChecks.clear();        // <-- reset lists used by All/None
  checkByName.clear();
  nameToId.clear();

  // nuke old side panel (if present) and build a fresh one
  Arrays.stream(getComponents())
      .filter(c -> "SIDE_PANEL".equals(c.getName()))
      .findFirst()
      .ifPresent(this::remove);

  JPanel side = buildSidePanel(labels);
  side.setName("SIDE_PANEL");
  add(side, BorderLayout.EAST);

  // redraw chart with ONLY current league’s series
  chart.clearAll();
  for (var e : seriesMap.entrySet()) {
    int teamId = e.getKey();
    String name = labels.getOrDefault(teamId, "T" + teamId);
    var pts = e.getValue();
    if (pts == null || pts.isEmpty()) continue;
    var weeks = pts.stream().map(WeekPoint::week).toList();
    var vals  = pts.stream().map(WeekPoint::value).toList();
    chart.addSeries(name, weeks, vals);
  }

  chart.setTitle("Rolling Form (Window " + windowN + "), League " + leagueId + " / " + season);
  revalidate();
  repaint();
});

  });
}

}


