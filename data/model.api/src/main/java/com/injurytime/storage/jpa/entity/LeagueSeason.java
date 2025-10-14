/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// LeagueSeason.java (projection table)
package com.injurytime.storage.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "LEAGUE_SEASON")
@IdClass(LeagueSeasonId.class)
public class LeagueSeason {
    @Id @Column(name = "LEAGUE_API_ID") private Integer leagueApiId;
    @Id @Column(name = "SEASON")        private Integer season;

    @Column(name = "SEASON_START", length = 30) private String seasonStart;
    @Column(name = "SEASON_END",   length = 30) private String seasonEnd;
    @Column(name = "IS_CURRENT")               private Boolean isCurrent;
    @Column(name = "COUNTRY", length = 30)     private String country;
    @Column(name = "COUNTRY_CODE", length = 5) private String countryCode;
    @Column(name = "COMP_TYPE", length = 50)   private String compType;

    // getters/setters …
}

