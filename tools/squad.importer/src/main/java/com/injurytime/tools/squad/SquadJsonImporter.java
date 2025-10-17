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
import jakarta.persistence.EntityManager;
import java.io.File;
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
  
  // --- result carrier for the toast/UI ---
public record ImportResult(int inserted, int updated, int skipped) {}

  /** Import a single team squad JSON into SQUAD_ROSTER for the given season. */
  // Import a single team squad JSON into SQUAD_ROSTER for the given season.
// Parses club id from the JSON block.
public ImportResult importFile(Path jsonFile, String seasonId) throws IOException {
  String json = Files.readString(jsonFile, java.nio.charset.StandardCharsets.UTF_8);
  JsonNode root = mapper.readTree(json);

  JsonNode resp = root.path("response");
  if (!resp.isArray() || resp.isEmpty()) return new ImportResult(0, 0, 0);

  JsonNode block = resp.get(0);
  int apiClubId = block.path("team").path("id").asInt();
  JsonNode players = block.path("players");
  if (!players.isArray()) return new ImportResult(0, 0, 0);

  final int[] inserted = {0};
  final int[] updated  = {0};
  final int[] skipped  = {0};

  jpa.tx(em -> {
    for (JsonNode p : players) {
      String  playerName = p.path("name").asText(null);
      int     playerApiId = p.path("id").asInt();
      String  posHuman = p.path("position").asText(null);
      String  posCode  = mapPosition(posHuman);
      Integer shirtNo  = p.hasNonNull("number") ? p.path("number").asInt() : null;

      // Optional fields – wire up if you start getting them from the JSON
      Boolean      onLoan       = null;
      Integer      loanFromClub = null;   // API club id of parent if on loan
      java.time.LocalDate      joinDate   = null;
      java.time.LocalDate      leaveDate  = null;
      java.time.LocalDateTime  updatedAt  = java.time.LocalDateTime.now();

      upsertPlayer(em, playerApiId, playerName);

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
        roster.setPositionCode(posCode);
        roster.setShirtNumber(shirtNo);
        roster.setOnLoan(onLoan);
        roster.setLoanFromClub(loanFromClub);
        roster.setJoinDate(joinDate);
        roster.setLeaveDate(leaveDate);
        roster.setUpdated(updatedAt);
        em.persist(roster);
        inserted[0]++;
      } else {
        boolean changed = false;
        changed |= setIfDiff(roster.getPositionCode(), posCode, roster::setPositionCode);
        changed |= setIfDiff(roster.getShirtNumber(),  shirtNo, roster::setShirtNumber);
        changed |= setIfDiff(roster.getOnLoan(),       onLoan, roster::setOnLoan);
        changed |= setIfDiff(roster.getLoanFromClub(), loanFromClub, roster::setLoanFromClub);
        changed |= setIfDiff(roster.getJoinDate(),     joinDate, roster::setJoinDate);
        changed |= setIfDiff(roster.getLeaveDate(),    leaveDate, roster::setLeaveDate);
        changed |= setIfDiff(roster.getUpdated(),      updatedAt, roster::setUpdated);

        if (changed) updated[0]++; else skipped[0]++;
      }
    }
    return null;
  });

  return new ImportResult(inserted[0], updated[0], skipped[0]);
}
  
  // Insert the player if missing. No schema is hard-coded; we rely on the connection's default schema.
private static void upsertPlayer(jakarta.persistence.EntityManager em, int playerApiId, String playerName) {
  Number n = (Number) em.createNativeQuery("SELECT COUNT(*) FROM PLAYER WHERE PLAYER_API_ID = ?")
      .setParameter(1, playerApiId)
      .getSingleResult();

  if (n.intValue() == 0) {
    em.createNativeQuery("INSERT INTO PLAYER (PLAYER_API_ID, PLAYER_NAME) VALUES (?, ?)")
        .setParameter(1, playerApiId)
        .setParameter(2, playerName)
        .executeUpdate();
  }
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
  

  /** NEW overload that the action calls: delegates to the original Path-based method. */
  // Overload used by the action; it delegates to the Path method.
// (clubApiId is shown in the toast; JSON still determines the club actually imported)
public ImportResult importFile(java.io.File jsonFile, String seasonId, int clubApiId) throws Exception {
  if (jsonFile == null) throw new IllegalArgumentException("jsonFile must not be null");
  // we return the same counts; the action can display clubApiId + seasonId alongside them
  return importFile(jsonFile.toPath(), seasonId);
}

// small utility to track “updated vs skipped”
private static <T> boolean setIfDiff(T cur, T val, java.util.function.Consumer<T> setter) {
  if (!java.util.Objects.equals(cur, val)) { setter.accept(val); return true; }
  return false;
}
}

