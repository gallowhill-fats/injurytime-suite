/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.tools.squad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.injurytime.storage.jpa.entity.*;
import jakarta.persistence.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Locale;

public final class SquadJsonImporter {

    private final EntityManagerFactory emf;
    private final ObjectMapper mapper = new ObjectMapper();

    public SquadJsonImporter(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /** Import a supplier squad JSON for one club into PLAYER / CLUB_SEASON / SQUAD_ROSTER. */
    public void importSquad(InputStream json, int leagueApiId, int season) throws IOException {
        JsonNode root = mapper.readTree(json);
        JsonNode first = root.path("response").path(0);
        if (first.isMissingNode()) throw new IOException("No response[0] in JSON");

        int apiClubId = first.path("team").path("id").asInt();
        String clubName = first.path("team").path("name").asText(null);

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // 1) Ensure the club exists (do NOT create here; CLUB is your canonical table)
            Club club = em.createQuery(
                    "SELECT c FROM Club c WHERE c.apiClubId = :id", Club.class)
                    .setParameter("id", apiClubId)
                    .getResultStream().findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "CLUB not found for API_CLUB_ID=" + apiClubId +
                            " (" + clubName + "). Please load clubs first."));

            // 2) Ensure (LEAGUE_API_ID, SEASON, API_CLUB_ID) membership exists
            ClubSeasonId csId = new ClubSeasonId(leagueApiId, season, apiClubId);
            ClubSeason cs = em.find(ClubSeason.class, csId);
            if (cs == null) {
                cs = new ClubSeason();
                cs.setLeagueApiId(leagueApiId);
                cs.setSeason(season);
                cs.setApiClubId(apiClubId);
                em.persist(cs);
            }

            // 3) Iterate players
            for (JsonNode p : first.withArray("players")) {
                int playerApiId = p.path("id").asInt();
                String playerName = p.path("name").asText(null);
                Integer shirt = p.path("number").isInt() ? p.path("number").asInt() : null;
                String pos = normalizePosition(p.path("position").asText(null));

                // Supplier sometimes puts 0 for id; skip those (cannot create canonical row)
                if (playerApiId <= 0) {
                    System.out.println("Skipping player with id=" + playerApiId + " name=" + playerName);
                    continue;
                }

                // 3a) Upsert PLAYER by PLAYER_API_ID
                Player player = em.createQuery(
                        "SELECT pl FROM Player pl WHERE pl.playerApiId = :pid", Player.class)
                        .setParameter("pid", playerApiId)
                        .getResultStream().findFirst().orElse(null);

                if (player == null) {
                    player = new Player();
                    // DB surrogate key (PLAYER_ID) is identity - JPA will fill it
                    player.setPlayerApiId(playerApiId);
                    player.setPlayerName(playerName != null ? playerName : ("Player#" + playerApiId));
                    em.persist(player);
                } else {
                    // keep supplier's latest display name if it changed
                    if (playerName != null && !playerName.equals(player.getPlayerName())) {
                        player.setPlayerName(playerName);
                        em.merge(player);
                    }
                }

                // 3b) Upsert SQUAD_ROSTER
                SquadRosterId rid = new SquadRosterId(leagueApiId, season, apiClubId, playerApiId);
                SquadRoster roster = em.find(SquadRoster.class, rid);
                if (roster == null) {
                    roster = new SquadRoster();
                    roster.setLeagueApiId(leagueApiId);
                    roster.setSeason(season);
                    roster.setApiClubId(apiClubId);
                    roster.setPlayerApiId(playerApiId);
                    roster.setPositionCode(pos);
                    roster.setShirtNumber(shirt);
                    roster.setUpdated(LocalDateTime.now());
                    em.persist(roster);
                } else {
                    roster.setPositionCode(pos);
                    roster.setShirtNumber(shirt);
                    roster.setUpdated(LocalDateTime.now());
                    em.merge(roster);
                }
            }

            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    private static String normalizePosition(String s) {
        if (s == null) return null;
        s = s.toLowerCase(Locale.ROOT).trim();
        return switch (s) {
            case "goalkeeper", "keeper", "gk" -> "GK";
            case "defender", "df" -> "DF";
            case "midfielder", "mf" -> "MF";
            case "attacker", "forward", "fw", "striker", "st" -> "FW";
            default -> null;
        };
    }
}

