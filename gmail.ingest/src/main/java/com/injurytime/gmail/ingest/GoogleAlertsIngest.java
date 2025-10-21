/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.gmail.ingest;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.injurytime.storage.api.JpaAccess;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.openide.util.Lookup;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public final class GoogleAlertsIngest {

  private final JpaAccess jpa;

  @Inject
  public GoogleAlertsIngest(JpaAccess jpa) {
    this.jpa = jpa;
  }

  public Result fetchAndStore(int maxMessages) throws Exception {
    Gmail service = GmailAuth.buildService();
    // Narrow to Alerts; tweak as needed (add "is:unread" if you like)
   // String q = "from:googlealerts-noreply@google.com";
    String q = "from:googlealerts-noreply@google.com newer_than:14d";
// Other handy filters:
// String q = "label:\"Google Alerts\" newer_than:30d";
// String q = "from:googlealerts-noreply@google.com is:unread";
// String q = "from:googlealerts-noreply@google.com after:2025/09/01";

    ListMessagesResponse list = service.users().messages().list("me")
        .setQ(q)
        .setMaxResults((long) maxMessages)
        .execute();

    int inserted = 0, skipped = 0, errors = 0;
    if (list.getMessages() == null || list.getMessages().isEmpty()) {
      return new Result(inserted, skipped, errors);
    }

    for (Message stub : list.getMessages()) {
      try {
        Message msg = service.users().messages().get("me", stub.getId()).setFormat("full").execute();

        String subject = header(msg, "Subject").orElse(null);
        String snippet = Optional.ofNullable(msg.getSnippet()).orElse(null);
        String html = extractBodyHtml(msg).orElse(null);
        String text = extractBodyText(msg).orElse(null);
        String displayUrl = "https://mail.google.com/mail/u/0/#inbox/" + msg.getId(); // convenience link
        long internalEpochMs = Optional.ofNullable(msg.getInternalDate()).orElse(0L);

        boolean ok = insertRaw(
            "gmail",
            msg.getId(),
            displayUrl,
            subject,
            html,
            text
        );
        if (ok) inserted++; else skipped++;
      } catch (Exception ex) {
        errors++;
      }
    }
    return new Result(inserted, skipped, errors);
  }

  private boolean insertRaw(String sourceSystem, String msgId, String sourceUri,
                          String subject, String rawHtml, String rawText) {
  return jpa.tx(em -> {
    int n = em.createNativeQuery("""
        INSERT INTO availability_events_raw
          (source_system, source_msg_id, source_uri, subject, raw_html, raw_text)
        VALUES
          (:sys, :mid, :uri, :subj, :html, :txt)
        ON CONFLICT (source_system, source_msg_id) DO NOTHING
      """)
      .setParameter("sys", sourceSystem)
      .setParameter("mid", msgId)
      .setParameter("uri", sourceUri)
      .setParameter("subj", subject)
      .setParameter("html", rawHtml)
      .setParameter("txt", rawText)
      .executeUpdate();
    return n > 0;
  });
}


  // Helpers to extract headers and parts
  private static Optional<String> header(Message msg, String name) {
    var payload = msg.getPayload();
    if (payload == null || payload.getHeaders() == null) return Optional.empty();
    return payload.getHeaders().stream()
        .filter(h -> name.equalsIgnoreCase(h.getName()))
        .map(MessagePartHeader::getValue)
        .findFirst();
  }

  private static Optional<String> extractBodyHtml(Message msg) {
    return extractBody(msg, "text/html");
  }

  private static Optional<String> extractBodyText(Message msg) {
    // prefer text/plain; if absent, fallback to stripping HTML later in your parser
    return extractBody(msg, "text/plain");
  }

  private static Optional<String> extractBody(Message msg, String mime) {
    var p = msg.getPayload();
    if (p == null) return Optional.empty();
    // Multipart
    if (p.getParts() != null && !p.getParts().isEmpty()) {
      for (MessagePart part : p.getParts()) {
        if (part.getMimeType() != null && part.getMimeType().startsWith(mime)) {
          var body = part.getBody();
          if (body != null && body.getData() != null) {
            byte[] bytes = Base64.getUrlDecoder().decode(body.getData());
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
          }
        }
      }
    }
    // Single part
    if (mime.equalsIgnoreCase(p.getMimeType())) {
      var body = p.getBody();
      if (body != null && body.getData() != null) {
        byte[] bytes = Base64.getUrlDecoder().decode(body.getData());
        return Optional.of(new String(bytes, StandardCharsets.UTF_8));
      }
    }
    return Optional.empty();
  }

  public record Result(int inserted, int skipped, int errors) {}
}

