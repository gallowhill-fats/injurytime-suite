/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.ui;

import com.injurytime.storage.api.JpaAccess;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

@ActionID(category = "Tools", id = "com.injurytime.analysis.ui.ShowLeagueTableAction")
@ActionRegistration(displayName = "#ShowLeagueTableAction_CTL_ShowLeagueTable")
@ActionReference(path = "Menu/Tools", position = 1600)
@Messages({
  "ShowLeagueTableAction_CTL_ShowLeagueTable=League Table…",
  "ShowLeagueTableAction_TTL_LeagueTable=League Table",
  "ShowLeagueTableAction_LBL_LeagueId=League ID:",
  "ShowLeagueTableAction_LBL_Season=Season:",
  "ShowLeagueTableAction_LBL_MaxWeek=Max week (blank = latest):",
  "ShowLeagueTableAction_BTN_Run=Run"
})
public final class ShowLeagueTableAction implements java.awt.event.ActionListener {

  private static final RequestProcessor RP = new RequestProcessor(ShowLeagueTableAction.class);

  @Inject
  private JpaAccess jpa; // optional @Inject; we'll also fallback to Lookup

  @Override
  public void actionPerformed(java.awt.event.ActionEvent e) {
    if (jpa == null) jpa = Lookup.getDefault().lookup(JpaAccess.class);
    if (jpa == null) {
      JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(),
          "No JpaAccess available.", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Small input form
    var leagueIdField = new JTextField(8);
    var seasonField   = new JTextField(8);
    var maxWeekField  = new JTextField(6);

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(4,6,4,6); gc.anchor = GridBagConstraints.WEST;
    gc.gridx=0; gc.gridy=0; form.add(new JLabel(Bundle.ShowLeagueTableAction_LBL_LeagueId()), gc);
    gc.gridx=1; form.add(leagueIdField, gc);
    gc.gridx=0; gc.gridy=1; form.add(new JLabel(Bundle.ShowLeagueTableAction_LBL_Season()), gc);
    gc.gridx=1; form.add(seasonField, gc);
    gc.gridx=0; gc.gridy=2; form.add(new JLabel(Bundle.ShowLeagueTableAction_LBL_MaxWeek()), gc);
    gc.gridx=1; form.add(maxWeekField, gc);

    int ok = JOptionPane.showConfirmDialog(
        WindowManager.getDefault().getMainWindow(),
        form, Bundle.ShowLeagueTableAction_TTL_LeagueTable(),
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

    if (ok != JOptionPane.OK_OPTION) return;

    final Integer leagueId, season, maxWeek;
    try {
      leagueId = Integer.valueOf(leagueIdField.getText().trim());
      season   = Integer.valueOf(seasonField.getText().trim());
      String mw = maxWeekField.getText().trim();
      maxWeek  = mw.isBlank() ? null : Integer.valueOf(mw);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(),
          "Please enter numeric League ID / Season (and week if used).",
          "Invalid input", JOptionPane.WARNING_MESSAGE);
      return;
    }

    // Run DB work off-EDT
    RP.post(() -> {
      try {
        var rows = jpa.tx((EntityManager em) -> {
          // league_table_as_of + join club name + form() in one go
          @SuppressWarnings("unchecked")
          List<Object[]> data = em.createNativeQuery("""
            SELECT
              t.team_api_id,
              COALESCE(c.club_name, 'Team '||t.team_api_id) AS team_name,
              t.played, t.won, t.drawn, t.lost,
              t.gf_home, t.ga_home, t.gf_away, t.ga_away,
              t.gf, t.ga, t.gd, t.pts,
              team_last_form(:lid,:season,t.team_api_id,5,:mw) AS form
            FROM league_table_as_of(:lid,:season,:mw) t
            LEFT JOIN club c ON c.api_club_id = t.team_api_id
            ORDER BY t.pts DESC, t.gd DESC, t.gf DESC, team_name
          """)
          .setParameter("lid", leagueId)
          .setParameter("season", season)
          .setParameter("mw", maxWeek)
          .getResultList();
          return data;
        });

        SwingUtilities.invokeLater(() -> showTableDialog(leagueId, season, maxWeek, rows));
      } catch (Exception ex) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(),
              "Failed to load league table:\n" + ex.getMessage(),
              "Error", JOptionPane.ERROR_MESSAGE));
      }
    });
  }

  // ----- UI -----
  private static void showTableDialog(int lid, int season, Integer mw, List<Object[]> rows) {
    LeagueTableModel model = new LeagueTableModel(rows);
    JTable table = new JTable(model);
    table.setAutoCreateRowSorter(true);
    table.setFillsViewportHeight(true);
    table.getColumnModel().getColumn(0).setPreferredWidth(36);  // #
    table.getColumnModel().getColumn(1).setPreferredWidth(180); // Team
    table.getColumnModel().getColumn(model.getColumnCount()-1).setPreferredWidth(110); // Form

    // colored renderer for Form col
    table.getColumnModel().getColumn(model.getColumnCount()-1)
        .setCellRenderer(new FormRenderer());

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JScrollPane(table), BorderLayout.CENTER);
    String title = "League " + lid + "  Season " + season +
                   (mw != null ? ("  (after week " + mw + ")") : "");
    JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(),
        panel, title, JOptionPane.PLAIN_MESSAGE);
  }

  // ----- Table model -----
  static final class LeagueTableModel extends AbstractTableModel {
    private final String[] cols = {
      "#","Team","P","W","D","L","GF(H)","GA(H)","GF(A)","GA(A)","GF","GA","GD","Pts","Form"
    };
    private final List<Object[]> data;

    LeagueTableModel(List<Object[]> rows) { this.data = rows; }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }

    @Override public Object getValueAt(int r, int c) {
      var x = data.get(r);
      // x indices: 0 team_api_id, 1 team_name, 2 played, 3 won, 4 drawn, 5 lost,
      // 6 gf_home, 7 ga_home, 8 gf_away, 9 ga_away, 10 gf, 11 ga, 12 gd, 13 pts, 14 form
      return switch (c) {
        case 0 -> r + 1;
        case 1 -> x[1];
        case 2 -> x[2];
        case 3 -> x[3];
        case 4 -> x[4];
        case 5 -> x[5];
        case 6 -> x[6];
        case 7 -> x[7];
        case 8 -> x[8];
        case 9 -> x[9];
        case 10 -> x[10];
        case 11 -> x[11];
        case 12 -> x[12];
        case 13 -> x[13];
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

  // ----- Colored badges for Form (W/D/L) -----
  static final class FormRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
      JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      String s = value == null ? "" : value.toString();
      lbl.setText(""); // we’ll draw badges
      lbl.setOpaque(true);
      lbl.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
      lbl.setHorizontalAlignment(SwingConstants.LEFT);

      // Build a small colored string like ● ● ● with tooltips
      StringBuilder html = new StringBuilder("<html>");
      for (int i = 0; i < s.length(); i++) {
        char ch = Character.toUpperCase(s.charAt(i));
        String color = switch (ch) {
          case 'W' -> "#2ecc71"; // green
          case 'D' -> "#f1c40f"; // yellow
          case 'L' -> "#e74c3c"; // red
          default -> "#bdc3c7";  // grey
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

