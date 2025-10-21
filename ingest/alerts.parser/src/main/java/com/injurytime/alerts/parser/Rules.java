/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.alerts.parser;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

public final class Rules {
  private static final Pattern PLAYER = Pattern.compile("\\b([A-Z][a-z]+(?:\\s[A-Z][a-z]+){0,2})\\b");
  private static final Pattern CLUB   = Pattern.compile("\\b(Man City|Manchester City|Altrincham|ALT|Arsenal)\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern STATUS = Pattern.compile("\\b(ruled out|out|doubtful|questionable|probable|available|suspended|illness)\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern REASON = Pattern.compile("\\b(hamstring|acl|knee|ankle|calf|thigh|groin|red card|yellow card|illness)\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern RETURN_DATE = Pattern.compile("\\b(\\d{1,2}\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec))\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern DURATION = Pattern.compile("\\b(\\d+)\\s+(day|days|week|weeks)\\b", Pattern.CASE_INSENSITIVE);

  public static List<ParsedClaim> extract(String subject, String text, String url) {
    String src = (subject + " " + text).replaceAll("\\s+", " ");
    String status = find(STATUS, src);
    String reason = find(REASON, src);
    LocalDate expected = parseReturn(src);
    Integer days = parseDuration(src);
    String type = normalizeTypeFrom(status, reason);

    // naive stubs — you’ll refine:
    String player = guessPlayer(src);
    String club = guessClub(src);

    int confidence = score(status, reason, expected, days);

    return List.of(new ParsedClaim(player, club, status, type, reason, null, expected, days, confidence, subject, snippet(src), url));
  }

  private static String find(Pattern p, String s){ var m=p.matcher(s); return m.find()?m.group(1):null; }
  private static String guessPlayer(String s){ /* you can match against players table cache */ return null; }
  private static String guessClub(String s){ var m=CLUB.matcher(s); return m.find()?m.group(1):null; }
  private static String normalizeTypeFrom(String status, String reason){
    if (reason!=null && reason.toLowerCase().contains("card")) return "suspension";
    if (reason!=null && List.of("illness","sick").contains(reason.toLowerCase())) return "illness";
    return "injury";
  }
  private static LocalDate parseReturn(String s){ /* parse dates */ return null; }
  private static Integer parseDuration(String s){ var m=DURATION.matcher(s); return m.find()?Integer.valueOf(m.group(1)):null; }
  private static int score(Object... bits){ int c=50; for(var b:bits) if(b!=null) c+=10; return Math.min(c,95); }
  private static String snippet(String s){ return s.length()>240? s.substring(0,240)+"…" : s; }
}

