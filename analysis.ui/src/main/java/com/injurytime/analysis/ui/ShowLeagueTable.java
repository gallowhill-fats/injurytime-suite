/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.ui;

import com.injurytime.analysis.api.LeagueTableService;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;

@ActionID(category="Tools", id="com.injurytime.analysis.ShowLeagueTable")
@ActionRegistration(displayName="#CTL_ShowLeagueTable")
@ActionReference(path="Menu/Tools", position=1600)
@Messages("CTL_ShowLeagueTable=Show League Table…")
public final class ShowLeagueTable implements ActionListener {
  @Override public void actionPerformed(ActionEvent e) {
    // prompt (hardcode for first pass)
    int leagueId = 39; // EPL
    int season   = 2024;
    Integer week = null; // or an Integer value to “as-of”

    var svc = Lookup.getDefault().lookup(LeagueTableService.class);
    var rows = svc.loadTable(leagueId, season, week);

    String[] cols = {"#", "Team", "P", "W", "D", "L",
                     "GF (H)", "GA (H)", "GF (A)", "GA (A)", "GD", "Pts", "Form"};
    Object[][] data = new Object[rows.size()][cols.length];

    for (int i=0;i<rows.size();i++) {
      var r = rows.get(i);
      data[i] = new Object[]{
        r.rank(), r.teamName(), r.played(), r.won(), r.drawn(), r.lost(),
        r.gfHome(), r.gaHome(), r.gfAway(), r.gaAway(), r.goalDiff(), r.points(), r.form5()
      };
    }

    var model = new javax.swing.table.DefaultTableModel(data, cols) {
      public boolean isCellEditable(int r,int c){return false;}
    };
    var table = new javax.swing.JTable(model);
    table.setAutoCreateRowSorter(true);
    table.setFillsViewportHeight(true);

    // Colour renderer for Form
    table.getColumn("Form").setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
      @Override protected void setValue(Object value) {
        String s = value == null ? "" : value.toString();
        // render spaced letters with colours
        var panel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        panel.setOpaque(true);
        panel.setBackground(java.awt.Color.WHITE);
        for (char ch : s.toCharArray()) {
          var lab = new javax.swing.JLabel(String.valueOf(ch));
          lab.setOpaque(true);
          switch (ch) {
            case 'W' -> lab.setBackground(new java.awt.Color(0xC8,0xE6,0xC9)); // green
            case 'D' -> lab.setBackground(new java.awt.Color(0xFF,0xF9,0xC4)); // yellow
            case 'L' -> lab.setBackground(new java.awt.Color(0xFF,0xCC,0xBC)); // red
            default  -> lab.setBackground(java.awt.Color.LIGHT_GRAY);
          }
          lab.setBorder(javax.swing.BorderFactory.createEmptyBorder(2,6,2,6));
          panel.add(lab);
        }
        setIcon(null); setText("");
        setHorizontalAlignment(LEFT);
        setOpaque(true);
        setLayout(new java.awt.BorderLayout());
        removeAll(); add(panel, java.awt.BorderLayout.WEST);
      }
    });

    var scroll = new javax.swing.JScrollPane(table);
    var dlg = new javax.swing.JDialog(WindowManager.getDefault().getMainWindow(), "League Table", false);
    dlg.getContentPane().add(scroll);
    dlg.setSize(900, 600);
    dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
    dlg.setVisible(true);
  }
}

