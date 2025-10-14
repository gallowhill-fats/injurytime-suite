/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.injurytime.ingest.store;

import org.injurytime.ingest.api.AvailabilityExtraction;

import java.sql.*;

public class JdbcStores {

    private final String url, user, pass;

    public JdbcStores(String url, String user, String pass)
    {
        this.url = url;
        this.user = user;
        this.pass = pass;
    }

    private Connection c() throws SQLException
    {
        return DriverManager.getConnection(url, user, pass);
    }

    public long saveRaw(String sourceSystem, String sourceId, String sourceUri, String subject, String html, String text) throws SQLException
    {
        try (var conn = c(); var ps = conn.prepareStatement(
                "INSERT INTO availability_events_raw (source_system, source_msg_id, source_uri, subject, raw_html, raw_text) VALUES (?,?,?,?,?,?) ON CONFLICT (source_system, source_msg_id) DO UPDATE SET raw_html=EXCLUDED.raw_html, raw_text=EXCLUDED.raw_text, subject=EXCLUDED.subject RETURNING id"))
        {
            ps.setString(1, sourceSystem);
            ps.setString(2, sourceId);
            ps.setString(3, sourceUri);
            ps.setString(4, subject);
            ps.setString(5, html);
            ps.setString(6, text);
            try (var rs = ps.executeQuery())
            {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public long insertEvent(long rawId, AvailabilityExtraction ex) throws SQLException
    {
        try (var conn = c(); var ps = conn.prepareStatement(
                "INSERT INTO availability_events (raw_id, player_id, club_id, availability_type, reason_subtype, status, start_date, expected_return_date, expected_duration_days, confidence, headline, snippet, canonical_article_url) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id"))
        {
            ps.setLong(1, rawId);
            if (ex.playerName() == null)
            {
                ps.setNull(2, Types.BIGINT);
            } else
            {
                ps.setNull(2, Types.BIGINT); // to be resolved later
            }
            ps.setNull(3, Types.BIGINT);
            ps.setString(4, ex.availabilityType());
            ps.setString(5, ex.reasonSubtype());
            ps.setString(6, ex.status());
            if (ex.startDate() == null)
            {
                ps.setNull(7, Types.DATE);
            } else
            {
                ps.setDate(7, Date.valueOf(ex.startDate()));
            }
            if (ex.expectedReturnDate() == null)
            {
                ps.setNull(8, Types.DATE);
            } else
            {
                ps.setDate(8, Date.valueOf(ex.expectedReturnDate()));
            }
            if (ex.expectedDurationDays() == null)
            {
                ps.setNull(9, Types.INTEGER);
            } else
            {
                ps.setInt(9, ex.expectedDurationDays());
            }
            ps.setInt(10, ex.confidence());
            ps.setString(11, ex.headline());
            ps.setString(12, ex.snippet());
            ps.setString(13, ex.canonicalUrl());
            try (var rs = ps.executeQuery())
            {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
