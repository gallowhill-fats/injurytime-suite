/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// SquadJsonImporter.java
package com.injurytime.tools.squad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.injurytime.storage.api.JpaAccess;
import com.injurytime.storage.jpa.entity.SquadRoster;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class SquadJsonImporter {
  private final JpaAccess jpa;
  private final ObjectMapper mapper = new ObjectMapper();

  // ✔ constructor your action must call
  public SquadJsonImporter(JpaAccess jpa) {
    if (jpa == null) throw new IllegalArgumentException("JpaAccess must not be null");
    this.jpa = jpa;
  }

  /** Import a single team squad JSON into SQUAD_ROSTER for the given season. */
  public int importFile(Path jsonFile, String seasonId) throws IOException {
    String json = Files.readString(jsonFile, StandardCharsets.ISO_8859_1);
    JsonNode root = mapper.readTree(json);

    JsonNode resp = root.path("response");
    if (!resp.isArray() || resp.isEmpty()) return 0;

    JsonNode block = resp.get(0);
    int apiClubId = block.path("team").path("id").asInt();
    JsonNode players = block.path("players");
    if (!players.isArray()) return 0;

    final int[] count = {0};
    jpa.tx(em -> {
      for (JsonNode p : players) {
        int playerApiId = p.path("id").asInt();
        String posHuman = p.path("position").asText(null);
        String posCode   = mapPosition(posHuman);
        Integer shirtNo  = p.hasNonNull("number") ? p.path("number").asInt() : null;

        // Optional fields (set to null for now; wire up if present in your JSON)
        Boolean onLoan = null;
        String loanFromClub = null;
        LocalDate joinDate = null;
        LocalDate leaveDate = null;
        LocalDateTime updatedAt = LocalDateTime.now();

        // find-or-create by (SEASON_ID, API_CLUB_ID, PLAYER_API_ID)
        SquadRoster roster = em.createQuery(
            "SELECT r FROM SquadRoster r " +
            "WHERE r.seasonId = :sid AND r.apiClubId = :cid AND r.playerApiId = :pid",
            SquadRoster.class)
          .setParameter("sid", seasonId)
          .setParameter("cid", apiClubId)
          .setParameter("pid", playerApiId)
          .getResultStream().findFirst().orElse(null);

        if (roster == null) {
          roster = new SquadRoster();
          roster.setSeasonId(seasonId);
          roster.setApiClubId(apiClubId);
          roster.setPlayerApiId(playerApiId);
          em.persist(roster);
        }

        roster.setPositionCode(posCode);
        roster.setShirtNumber(shirtNo);
        roster.setOnLoan(onLoan);
        roster.setLoanFromClub(loanFromClub);
        roster.setJoinDate(joinDate);
        roster.setLeaveDate(leaveDate);
        roster.setUpdated(updatedAt);

        count[0]++;
      }
      return null;
    });

    return count[0];
  }

  private static String mapPosition(String human) {
    if (human == null) return null;
    switch (human.toLowerCase()) {
      case "goalkeeper": return "GK";
      case "defender":   return "DF";
      case "midfielder": return "MF";
      case "attacker":
      case "forward":    return "FW";
      default:           return human;
    }
  }
}
