/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.pg.tools.alias;

import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import org.openide.awt.*;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Lookup;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@ActionID(category="Tools", id="com.injurytime.pg.tools.alias.ImportAliasesAction")
@ActionRegistration(displayName="#CTL_ImportAliasesAction", asynchronous=true)
@ActionReference(path="Menu/Tools", position=1850)
@Messages("CTL_ImportAliasesAction=Import Player Aliases (CSV folder)…")
public final class ImportAliasesAction implements ActionListener {
  private final JpaAccess jpa = Lookup.getDefault().lookup(JpaAccess.class);

  @Override public void actionPerformed(ActionEvent e) {
    JFileChooser ch = new JFileChooser();
    ch.setDialogTitle("Pick folder with alias CSVs");
    ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    if (ch.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;

    Path dir = ch.getSelectedFile().toPath();

    int files = 0, rows = 0, aliases = 0, skipped = 0, errors = 0;

    try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.csv")) {
      for (Path csv : ds) {
        files++;
        try (BufferedReader br = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
          String header = br.readLine(); // skip header
          String line;
          List<String[]> batch = new ArrayList<>(512);

          while ((line = br.readLine()) != null) {
            String[] cols = parseCsvLine(line);
            // Expecting: api_club_id, player_api_id, player_name, player_alias
            if (cols.length < 4) { skipped++; continue; }

            String playerIdStr = cols[1].trim();
            String playerName  = cols[2] == null ? "" : cols[2].trim();
            String aliasStr    = cols[3] == null ? "" : cols[3].trim();

            Integer playerId = null;
            try { playerId = Integer.valueOf(playerIdStr); } catch (NumberFormatException ex) { skipped++; continue; }

            // split aliases by comma, keep quoted content intact (parseCsvLine already did)
            List<String> alist = new ArrayList<>();
            if (!aliasStr.isBlank()) {
              for (String a : aliasStr.split("\\s*,\\s*")) {
                String norm = norm(a);
                if (norm != null) alist.add(norm);
              }
            }
            // Optional: also include normalized player_name as an alias
            String normName = norm(playerName);
            if (normName != null) alist.add(normName);

            if (alist.isEmpty()) { rows++; continue; }

            for (String a : alist) {
              batch.add(new String[]{ playerId.toString(), a });
              if (batch.size() >= 500) {
                aliases += upsertBatch(batch);
                batch.clear();
              }
            }
            rows++;
          }
          if (!batch.isEmpty()) aliases += upsertBatch(batch);
        } catch (Exception ex) {
          errors++;
        }
      }
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(null, "Failed to read directory: " + ex.getMessage(),
          "Alias Import", JOptionPane.ERROR_MESSAGE);
      return;
    }

    String msg = String.format("Processed %d files, %d rows, %d aliases inserted, %d skipped, %d errors",
        files, rows, aliases, skipped, errors);
    JOptionPane.showMessageDialog(null, msg, "Alias Import", JOptionPane.INFORMATION_MESSAGE);
  }

  private int upsertBatch(List<String[]> batch) {
    if (batch.isEmpty()) return 0;
    return jpa.tx((EntityManager em) -> {
      int inserted = 0;
      for (String[] rec : batch) {
        Integer pid = Integer.valueOf(rec[0]);
        String alias = rec[1];
        inserted += em.createNativeQuery("""
          INSERT INTO player_alias(player_api_id, alias)
          VALUES (:pid, :alias)
          ON CONFLICT (player_api_id, alias) DO NOTHING
        """)
            .setParameter("pid", pid)
            .setParameter("alias", alias)
            .executeUpdate();
      }
      return inserted;
    });
  }

  // normalize: trim, collapse whitespace, lowercase; return null if empty
  private static String norm(String s) {
    if (s == null) return null;
    String t = s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    return t.isBlank() ? null : t;
  }

  // Tiny CSV parser for one line (handles quotes + commas inside quotes)
  private static String[] parseCsvLine(String line) {
    List<String> out = new ArrayList<>();
    StringBuilder cur = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '\"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i+1) == '\"') {
          cur.append('\"'); i++; // escaped quote
        } else {
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        out.add(cur.toString());
        cur.setLength(0);
      } else {
        cur.append(c);
      }
    }
    out.add(cur.toString());
    return out.toArray(new String[0]);
  }
}
