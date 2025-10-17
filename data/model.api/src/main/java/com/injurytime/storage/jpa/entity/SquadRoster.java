/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// SquadRoster.java
package com.injurytime.storage.jpa.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "SQUAD_ROSTER")
@IdClass(SquadRosterId.class)
public class SquadRoster {

  // ---- PRIMARY KEY ----
  @Id
  @Column(name = "SEASON_ID", nullable = false)
  private String seasonId;

  @Id
  @Column(name = "API_CLUB_ID", nullable = false)
  private Integer apiClubId;

  @Id
  @Column(name = "PLAYER_API_ID", nullable = false)
  private Integer playerApiId;

  // ---- NON-KEY FIELDS ----
  @Column(name = "POSITION_CODE")
  private String positionCode;

  @Column(name = "SHIRT_NUMBER")
  private Integer shirtNumber;

  @Column(name = "ON_LOAN")
  private Boolean onLoan;

  // If this is an INT in DB, change type to Integer and keep the @Column as-is.
  @Column(name = "LOAN_FROM_CLUB")
  private Integer loanFromClub;

  @Column(name = "JOIN_DATE")
  private LocalDate joinDate;

  @Column(name = "LEAVE_DATE")
  private LocalDate leaveDate;

  @Column(name = "UPDATED")
  private LocalDateTime updated;

  // ---- CTOR ----
  public SquadRoster() {}

  // ---- GETTERS/SETTERS (canonical names) ----
  public String getSeasonId() { return seasonId; }
  public void setSeasonId(String seasonId) { this.seasonId = seasonId; }

  public Integer getApiClubId() { return apiClubId; }
  public void setApiClubId(int apiClubId) { this.apiClubId = apiClubId; }

  public Integer getPlayerApiId() { return playerApiId; }
  public void setPlayerApiId(int playerApiId) { this.playerApiId = playerApiId; }

  public String getPositionCode() { return positionCode; }
  public void setPositionCode(String positionCode) { this.positionCode = positionCode; }

  public Integer getShirtNumber() { return shirtNumber; }
  public void setShirtNumber(Integer shirtNumber) { this.shirtNumber = shirtNumber; }

  public Boolean getOnLoan() { return onLoan; }
  public void setOnLoan(Boolean onLoan) { this.onLoan = onLoan; }

  public Integer getLoanFromClub() { return loanFromClub; }
  public void setLoanFromClub(Integer loanFromClub) { this.loanFromClub = loanFromClub; }

  public LocalDate getJoinDate() { return joinDate; }
  public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }

  public LocalDate getLeaveDate() { return leaveDate; }
  public void setLeaveDate(LocalDate leaveDate) { this.leaveDate = leaveDate; }

  public LocalDateTime getUpdated() { return updated; }
  public void setUpdated(LocalDateTime updated) { this.updated = updated; }
}


