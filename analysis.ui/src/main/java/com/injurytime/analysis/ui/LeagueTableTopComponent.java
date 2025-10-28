/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.ui;

import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

@TopComponent.Description(
    preferredID = "LeagueTableTopComponent",
    persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "com.injurytime.analysis.ui.LeagueTableTopComponent")
@ActionReference(path = "Menu/Window", position = 650)
@TopComponent.OpenActionRegistration(
    displayName = "CTL_LeagueTableLive",
    preferredID = "LeagueTableTopComponent"
)
@Messages({
  "LeagueTableTopComponent_Title=League Table (Live)",
  "LeagueTableTopComponent_LBL_LeagueId=League:",
  "LeagueTableTopComponent_LBL_Season=Season:",
  "LeagueTableTopComponent_BTN_Load=Load",
  "LeagueTableTopComponent_LBL_Week=Week:"
})
public final class LeagueTableTopComponent extends TopComponent {

  // --- services
  private final RequestProcessor RP = new RequestProcessor(LeagueTableTopComponent.class);
  private JpaAccess jpa;

  // --- UI
  private final JTextField leagueField = new JTextField(6);
  private final JTextField seasonField = new JTextField(6);
  private final JButton loadBtn = new JButton(Bundle.LeagueTableTopComponent_BTN_Load());
  private final JLabel  weekLbl = new JLabel(Bundle.LeagueTableTopComponent_LBL_Week());
  private final JSlider weekSlider = new JSlider(); // set bounds after load
  private final JLabel  weekValue = new JLabel("-");
  private final JTable  table = new JTable();
  private final LeagueTableModel model = new LeagueTableModel();

  // state
  private Integer leagueId, season;
  private Integer maxWeek;                 // discovered from DB
  private volatile boolean sliderBusy;     // guard against floods

  public LeagueTableTopComponent() {
    setName(Bundle.LeagueTableTopComponent_Title());
    setLayout(new BorderLayout());
    setPreferredSize(new Dimension(1000, 580));  // bigger panel

    // top controls
    JPanel north = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6,8,6,8);
    gc.gridy = 0; gc.gridx = 0; north.add(new JLabel(Bundle.LeagueTableTopComponent_LBL_LeagueId()), gc);
    gc.gridx = 1; north.add(leagueField, gc);
    gc.gridx = 2; north.add(new JLabel(Bundle.LeagueTableTopComponent_LBL_Season()), gc);
    gc.gridx = 3; north.add(seasonField, gc);
    gc.gridx = 4; north.add(loadBtn, gc);

    gc.gridy = 1; gc.gridx = 0;
    north.add(weekLbl, gc);
    gc.gridx = 1; gc.gridwidth = 3;
    weekSlider.setEnabled(false); // until loaded
    north.add(weekSlider, gc);
    gc.gridx = 4; gc.gridwidth = 1;
    north.add(weekValue, gc);

    add(north, BorderLayout.NORTH);

    // table
    table.setModel(model);
    table.setAutoCreateRowSorter(true);
    table.setFillsViewportHeight(true);
    table.getTableHeader().setReorderingAllowed(false);
    add(new JScrollPane(table), BorderLayout.CENTER);

    // form column coloring
    table.setDefaultRenderer(String.class, new FormAwareRenderer());
    // nudge some widths
    SwingUtilities.invokeLater(() -> {
      if (table.getColumnCount() > 0) {
        table.getColumnModel().getColumn(0).setPreferredWidth(36);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(model.getColumnCount()-1).setPreferredWidth(130);
      }
    });

    // actions
    loadBtn.addActionListener(this::doLoad);
    weekSlider.addChangeListener(new ChangeListener() {
      @Override public void stateChanged(ChangeEvent e) {
        if (weekSlider.getValueIsAdjusting()) return; // wait until settle
        if (sliderBusy) return;
        sliderBusy = true;
        int wk = weekSlider.getValue();
        weekValue.setText(String.valueOf(wk));
        RP.post(() -> {
          try {
            loadTable(leagueId, season, wk == 0 ? null : wk);
          } finally {
            sliderBusy = false;
          }
        }, 120);
      }
    });
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (jpa == null) {
      jpa = Lookup.getDefault().lookup(JpaAccess.class);
    }
  }

  // --- actions ---

  private void doLoad(ActionEvent evt) {
    try {
      leagueId = Integer.valueOf(leagueField.getText().trim());
      season   = Integer.valueOf(seasonField.getText().trim());
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "Enter numeric League + Season.", "Input", JOptionPane.WARNING_MESSAGE);
      return;
    }

    // Discover max week from fixtures
    RP.post(() -> {
      Integer discoveredMax = jpa.tx((EntityManager em) -> {
        Number n = (Number) em.createNativeQuery("""
          SELECT max(fixture_week_no(round_name)) 
          FROM fixture 
          WHERE league_id=:lid AND season=:s
        """).setParameter("lid", leagueId)
          .setParameter("s", season)
          .getSingleResult();
        return n == null ? null : n.intValue();
      });

      this.maxWeek = discoveredMax == null ? 0 : discoveredMax;
      SwingUtilities.invokeLater(() -> {
        // Slider: 0 = latest; 1..maxWeek = week filter
        int mx = Math.max(0, this.maxWeek);
        weekSlider.setEnabled(true);
        weekSlider.setMinimum(0);
        weekSlider.setMaximum(mx);
        weekSlider.setMajorTickSpacing(Math.max(1, mx / 10));
        weekSlider.setPaintTicks(true);
        weekSlider.setValue(0);
        weekValue.setText("latest");
      });

      // initial load = latest (null week filter)
      loadTable(leagueId, season, null);
    });
  }

  private void loadTable(int leagueId, int season, Integer weekOrNull) {
    List<Object[]> rows = jpa.tx((EntityManager em) -> {
      @SuppressWarnings("unchecked")
      List<Object[]> data = em.createNativeQuery("""
        SELECT
          t.team_api_id,
          COALESCE(c.club_name, 'Team '||t.team_api_id) AS team_name,
          t.played, t.won, t.drawn, t.lost,
          t.gf_home, t.ga_home, t.gf_away, t.ga_away,
          t.gf, t.ga, t.gd, t.pts,
          COALESCE(team_last_form(:lid,:season,t.team_api_id,5,:mw), '') AS form
        FROM league_table_as_of(:lid,:season,:mw) t
        LEFT JOIN club c ON c.api_club_id = t.team_api_id
        ORDER BY t.pts DESC, t.gd DESC, t.gf DESC, team_name
      """)
      .setParameter("lid", leagueId)
      .setParameter("season", season)
      .setParameter("mw", weekOrNull)
      .getResultList();
      return data;
    });

    SwingUtilities.invokeLater(() -> {
      model.setData(rows);
      // update title
      String t = "League " + leagueId + "  Season " + season
          + (weekOrNull == null ? "  (latest)" : "  (after week " + weekOrNull + ")");
      setDisplayName(t);
    });
  }

  // --- model & renderers ---

  static final class LeagueTableModel extends AbstractTableModel {
    private final String[] cols = {
      "#","Team","P","W","D","L","GF(H)","GA(H)","GF(A)","GA(A)","GF","GA","GD","Pts","Form"
    };
    private List<Object[]> data = java.util.List.of();

    void setData(List<Object[]> rows) {
      this.data = rows;
      fireTableDataChanged();
    }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }

    @Override public Object getValueAt(int r, int c) {
      var x = data.get(r);
      return switch (c) {
        case 0 -> r + 1;             // rank
        case 1 -> x[1];              // team_name
        case 2 -> x[2]; case 3 -> x[3]; case 4 -> x[4]; case 5 -> x[5];
        case 6 -> x[6]; case 7 -> x[7]; case 8 -> x[8]; case 9 -> x[9];
        case 10 -> x[10]; case 11 -> x[11]; case 12 -> x[12]; case 13 -> x[13];
        case 14 -> Objects.toString(x[14], "");
        default -> "";
      };
    }
    @Override public Class<?> getColumnClass(int c) {
      return switch (c) {
        case 0,2,3,4,5,6,7,8,9,10,11,12,13 -> Integer.class;
        default -> String.class;
      };
    }
    @Override public boolean isCellEditable(int r, int c) { return false; }
  }

  /** Colors only the "Form" column; lets others render normally. */
  static final class FormAwareRenderer extends DefaultTableCellRenderer {
    @Override public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

      // if it's not the last column ("Form"), delegate to default
      if (column != table.getColumnCount() - 1) {
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      }

      JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
      String s = value == null ? "" : value.toString();
      lbl.setOpaque(true);
      lbl.setHorizontalAlignment(SwingConstants.LEFT);
      lbl.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

      StringBuilder html = new StringBuilder("<html>");
      for (int i = 0; i < s.length(); i++) {
        char ch = Character.toUpperCase(s.charAt(i));
        String color = switch (ch) {
          case 'W' -> "#2ecc71"; // green
          case 'D' -> "#f1c40f"; // yellow
          case 'L' -> "#e74c3c"; // red
          default -> "#bdc3c7";  // grey (unknown)
        };
        html.append("<span style='color:").append(color).append(";'>&#9679;</span>&nbsp;");
      }
      html.append("</html>");
      lbl.setText(html.toString());
      lbl.setToolTipText(s.replace("", " ").trim());
      return lbl;
    }
  }
}

