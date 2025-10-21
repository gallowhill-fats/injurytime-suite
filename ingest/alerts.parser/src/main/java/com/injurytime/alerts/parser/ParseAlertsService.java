/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.alerts.parser;

import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

public final class ParseAlertsService {
    
    
    private static final JaroWinklerSimilarity JW = new JaroWinklerSimilarity();

    private final JpaAccess jpa;

    public ParseAlertsService(JpaAccess jpa)
    {
        this.jpa = jpa;
    }

    public record Result(int inserted, int updated, int skipped, int errors) {

    }

    public Result run(int daysBack) {
  // 1) First, fetch the raw rows (single tx just to read them)
  final List<Object[]> raws = jpa.tx(em -> {
    Instant cutoff = Instant.now().minus(Duration.ofDays(daysBack));
    return em.createNativeQuery("""
        SELECT id, subject, coalesce(raw_text, '') AS txt, source_uri
        FROM availability_events_raw
        WHERE fetched_at >= :cutoff
        ORDER BY id
      """)
      .setParameter("cutoff", Timestamp.from(cutoff))
      .getResultList();
  });

  int ins = 0, upd = 0, sk = 0, err = 0;

  // 2) Handle each raw in its own transaction
  for (Object[] r : raws) {
    final long rawId     = ((Number) r[0]).longValue();
    final String subject = (String) r[1];
    final String text    = (String) r[2];
    final String uri     = (String) r[3];

    try {
        
        // 1) Load raw_html for this raw_id (we fetched only text earlier)
final String rawHtml = jpa.tx(em2 ->
  (String) em2.createNativeQuery("SELECT raw_html FROM availability_events_raw WHERE id = :id")
              .setParameter("id", rawId)
              .getSingleResult()
);

// 2) Build candidates: HTML items + subject/text fallback
List<HtmlAlertExtractor.Item> items = HtmlAlertExtractor.extractItems(rawHtml);
if (items.isEmpty()) {
  items = List.of(new HtmlAlertExtractor.Item(subject, text, uri));
}

for (HtmlAlertExtractor.Item it : items) {
  var facts = Rules.extract(it.title(), it.snippet() != null ? it.snippet() : "", it.url());
  if (facts == null || facts.isEmpty()) { continue; }

  for (ParsedClaim c : facts) {
    try {
      jpa.tx(em -> {
        Integer clubApiId   = resolveClubApiId(em, c.clubName());
        Integer playerApiId = resolvePlayerApiId(em, c.playerName(), clubApiId);

        if (playerApiId == null && clubApiId == null) throw new SkipClaim();

        String nStatus = (String) em.createNativeQuery("SELECT normalize_status(:s)")
            .setParameter("s", c.status()).getSingleResult();
        String nType = (String) em.createNativeQuery("SELECT normalize_availability_type(:t)")
            .setParameter("t", c.type()).getSingleResult();

        Number eventIdNum = (Number) em.createNativeQuery("""
          INSERT INTO availability_events(
            raw_id, player_api_id, api_club_id,
            availability_type, reason_subtype, status,
            start_date, expected_return_date, expected_duration_days,
            confidence, headline, snippet, canonical_article_url
          )
          VALUES (
            :raw, :pid, :cid,
            :atype, :reason, :status,
            :start, :expected, :dur,
            :conf, :head, :snip, :url
          )
          RETURNING id
        """)
        .setParameter("raw", rawId)
        .setParameter("pid", playerApiId)
        .setParameter("cid", clubApiId)
        .setParameter("atype", nType)
        .setParameter("reason", c.reason())
        .setParameter("status", nStatus)
        .setParameter("start", c.startDate())
        .setParameter("expected", c.expectedReturn())
        .setParameter("dur", c.expectedDays())
        .setParameter("conf", c.confidence())
        .setParameter("head", c.headline() != null ? c.headline() : it.title())
        .setParameter("snip", c.snippet() != null ? c.snippet() : it.snippet())
        .setParameter("url", it.url())
        .getSingleResult();

        long eventId = eventIdNum.longValue();

        if (playerApiId != null) {
          em.createNativeQuery("""
            INSERT INTO player_availability_current(
              player_api_id, status, availability_type, reason_subtype, updated_at, source_event_id, expected_return_date
            )
            VALUES (:pid, :status, :atype, :reason, now(), :eid, :expected)
            ON CONFLICT (player_api_id) DO UPDATE
              SET status               = EXCLUDED.status,
                  availability_type    = EXCLUDED.availability_type,
                  reason_subtype       = EXCLUDED.reason_subtype,
                  updated_at           = now(),
                  source_event_id      = EXCLUDED.source_event_id,
                  expected_return_date = EXCLUDED.expected_return_date
          """)
          .setParameter("pid", playerApiId)
          .setParameter("status", nStatus)
          .setParameter("atype", nType)
          .setParameter("reason", c.reason())
          .setParameter("eid", eventId)
          .setParameter("expected", c.expectedReturn())
          .executeUpdate();
        }
        return null;
      });

      ins++;
      if (c.playerName() != null) upd++;
    } catch (SkipClaim sc) {
      sk++;
    } catch (Exception perClaim) {
      err++;
    }
  }
}
        
        
      var facts = Rules.extract(subject, text, uri); // List<ParsedClaim>
      if (facts == null || facts.isEmpty()) { sk++; continue; }

      for (ParsedClaim c : facts) {
        try {
          jpa.tx(em -> {
            // resolve API IDs (not surrogate PKs)
            Integer clubApiId   = resolveClubApiId(em, c.clubName());
            Integer playerApiId = resolvePlayerApiId(em, c.playerName(),clubApiId);
            
            if (playerApiId == null && clubApiId == null) {
              // nothing to anchor on → skip this claim
              throw new SkipClaim();
            }

            // normalize via SQL helpers (they tolerate nulls and map to 'unknown')
            String nStatus = (String) em.createNativeQuery("SELECT normalize_status(:s)")
                .setParameter("s", c.status()).getSingleResult();
            String nType   = (String) em.createNativeQuery("SELECT normalize_availability_type(:t)")
                .setParameter("t", c.type()).getSingleResult();

            // INSERT event and capture ID with RETURNING
            Number eventIdNum = (Number) em.createNativeQuery("""
              INSERT INTO availability_events(
                raw_id, player_api_id, api_club_id,
                availability_type, reason_subtype, status,
                start_date, expected_return_date, expected_duration_days,
                confidence, headline, snippet, canonical_article_url
              )
              VALUES (
                :raw, :pid, :cid,
                :atype, :reason, :status,
                :start, :expected, :dur,
                :conf, :head, :snip, :url
              )
              RETURNING id
            """)
            .setParameter("raw", rawId)
            .setParameter("pid", playerApiId)
            .setParameter("cid", clubApiId)
            .setParameter("atype", nType)
            .setParameter("reason", c.reason())
            .setParameter("status", nStatus)
            .setParameter("start", c.startDate())
            .setParameter("expected", c.expectedReturn())
            .setParameter("dur", c.expectedDays())
            .setParameter("conf", c.confidence())
            .setParameter("head", c.headline())
            .setParameter("snip", c.snippet())
            .setParameter("url", c.url())
            .getSingleResult();

            long eventId = eventIdNum.longValue();

            // Roll up current ONLY if we have a player (the table is keyed by player_api_id)
            if (playerApiId != null) {
              em.createNativeQuery("""
                INSERT INTO player_availability_current(
                  player_api_id, status, availability_type, reason_subtype, updated_at, source_event_id, expected_return_date
                )
                VALUES (:pid, :status, :atype, :reason, now(), :eid, :expected)
                ON CONFLICT (player_api_id) DO UPDATE
                  SET status               = EXCLUDED.status,
                      availability_type    = EXCLUDED.availability_type,
                      reason_subtype       = EXCLUDED.reason_subtype,
                      updated_at           = now(),
                      source_event_id      = EXCLUDED.source_event_id,
                      expected_return_date = EXCLUDED.expected_return_date
              """)
              .setParameter("pid", playerApiId)
              .setParameter("status", nStatus)
              .setParameter("atype", nType)
              .setParameter("reason", c.reason())
              .setParameter("eid", eventId)
              .setParameter("expected", c.expectedReturn())
              .executeUpdate();
            }

            return null;
          });

          ins++;           // event inserted
          if (c.playerName() != null) upd++;  // rolled up (best-effort counter)
        } catch (SkipClaim sc) {
          sk++;
        } catch (Exception perClaim) {
          err++;
        }
      }
    } catch (Exception ex) {
      // any failure extracting facts for this raw
      err++;
    }
  }

  return new Result(ins, upd, sk, err);
}

// tiny local signal exception for flow control
private static final class SkipClaim extends RuntimeException {}


   private Integer resolvePlayerApiId(EntityManager em, String rawPlayerName, Integer clubApiId) {
  if (rawPlayerName == null || rawPlayerName.isBlank()) return null;
  String name = rawPlayerName.trim();

  // 1) alias / exact
  var best = em.createNativeQuery("""
      SELECT p.player_api_id
      FROM player_alias a JOIN player p ON p.player_api_id = a.player_api_id
      WHERE lower(a.alias) = lower(:n)
      UNION
      SELECT p.player_api_id
      FROM player p
      WHERE lower(p.player_name) = lower(:n)
      LIMIT 1
    """).setParameter("n", name)
      .getResultStream().findFirst().orElse(null);
  if (best != null) return ((Number) best).intValue();

  // 2) fuzzy, constrained to the club roster (if we know the club)
  if (clubApiId != null) {
    @SuppressWarnings("unchecked")
    var rows = em.createNativeQuery("""
        SELECT p.player_api_id, p.player_name
        FROM squad_roster r
        JOIN player p ON p.player_api_id = r.player_api_id
        WHERE r.api_club_id = :cid
      """).setParameter("cid", clubApiId).getResultList();

    double bestScore = 0.0;
    Integer bestPid = null;
    String ln = name.toLowerCase();

    for (Object row : rows) {
      Object[] arr = (Object[]) row;
      Integer pid = ((Number) arr[0]).intValue();
      String pname = ((String) arr[1]);
      double score = JW.apply(ln, pname.toLowerCase());
      if (pname.toLowerCase().contains(ln)) score += 0.1; // small boost
      if (score > bestScore) { bestScore = score; bestPid = pid; }
    }
    if (bestScore >= 0.90) return bestPid; // conservative threshold
  }
  return null;
}

    private Integer resolveClubApiId(EntityManager em, String clubText) {
  if (clubText == null || clubText.isBlank()) return null;
  var r = em.createNativeQuery("""
      SELECT api_club_id
      FROM club
      WHERE lower(club_name) = lower(:n)
         OR lower(club_abbr) = lower(:n)
      LIMIT 1
    """).setParameter("n", clubText.trim()).getResultStream().findFirst().orElse(null);
  return r == null ? null : ((Number) r).intValue();
}


}
