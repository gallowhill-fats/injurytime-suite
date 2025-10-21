/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.alerts.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

public final class HtmlAlertExtractor {

  public record Item(String title, String snippet, String url) {}

  /** Very tolerant: finds “card-like” blocks, pulls title/snippet/link. */
  public static List<Item> extractItems(String html) {
    List<Item> out = new ArrayList<>();
    if (html == null || html.isBlank()) return out;

    Document doc = Jsoup.parse(html);

    // Try several selectors — Google changes markup over time.
    // 1) Common: every article block often has an anchor + snippet paragraph/div
    for (Element card : doc.select("table,div,article,td,section")) {
      Element a = card.selectFirst("a[href]");
      if (a == null) continue;
      String title = safe(a.text());
      if (title.isEmpty()) continue;

      String href = a.absUrl("href");
      if (href.isEmpty()) href = a.attr("href");

      // Nearby snippet text
      Element sn = card.selectFirst("p, div, span");
      String snippet = sn != null ? safe(sn.text()) : null;

      out.add(new Item(title, snippet, href));
    }

    // Fallback: plain anchors in the whole doc
    if (out.isEmpty()) {
      for (Element a : doc.select("a[href]")) {
        String t = safe(a.text());
        if (t.length() > 0) {
          String href = a.absUrl("href");
          if (href.isEmpty()) href = a.attr("href");
          out.add(new Item(t, null, href));
        }
      }
    }
    return out;
  }

  private static String safe(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", " "); }
}

