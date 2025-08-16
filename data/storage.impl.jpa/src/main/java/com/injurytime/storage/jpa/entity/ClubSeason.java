/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// ClubSeason.java
package com.injurytime.storage.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "CLUB_SEASON")
@IdClass(ClubSeasonId.class)
public class ClubSeason {
    @Id @Column(name = "LEAGUE_API_ID") private Integer leagueApiId;
    @Id @Column(name = "SEASON")        private Integer season;
    @Id @Column(name = "API_CLUB_ID")   private Integer apiClubId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "API_CLUB_ID", referencedColumnName = "API_CLUB_ID", insertable=false, updatable=false)
    private Club club;

    // getters/setters …
}

