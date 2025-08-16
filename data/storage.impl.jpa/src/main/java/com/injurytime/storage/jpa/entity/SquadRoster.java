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
    @Id @Column(name = "LEAGUE_API_ID")  private Integer leagueApiId;
    @Id @Column(name = "SEASON")         private Integer season;
    @Id @Column(name = "API_CLUB_ID")    private Integer apiClubId;
    @Id @Column(name = "PLAYER_API_ID")  private Integer playerApiId;

    @Column(name = "POSITION_CODE", length = 12) private String positionCode;
    @Column(name = "SHIRT_NUMBER")               private Integer shirtNumber;
    @Column(name = "ON_LOAN", length = 1)        private String onLoan;          // 'Y'/'N'
    @Column(name = "LOAN_FROM_CLUB")             private Integer loanFromClub;
    @Column(name = "JOIN_DATE")                  private LocalDate joinDate;
    @Column(name = "LEAVE_DATE")                 private LocalDate leaveDate;
    @Column(name = "UPDATED")                    private LocalDateTime updated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLAYER_API_ID", referencedColumnName = "PLAYER_API_ID", insertable=false, updatable=false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "API_CLUB_ID", referencedColumnName = "API_CLUB_ID", insertable=false, updatable=false)
    private Club club;

    // getters/setters …
}

