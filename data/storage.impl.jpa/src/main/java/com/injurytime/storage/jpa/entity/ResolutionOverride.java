/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// ResolutionOverride.java
package com.injurytime.storage.jpa.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "RESOLUTION_OVERRIDES")
@IdClass(ResolutionOverrideId.class)
public class ResolutionOverride {

    @Id
    @Column(name = "LEAGUE_API_ID")
    private Integer leagueApiId;

    @Id
    @Column(name = "SEASON")
    private Integer season;

    @Id
    @Column(name = "RAW_TEXT", length = 200)
    private String rawText;

    @Column(name = "PLAYER_API_ID")
    private Integer playerApiId;

    @Column(name = "API_CLUB_ID")
    private Integer apiClubId;

    @Column(name = "NOTES", length = 200)
    private String notes;

    public ResolutionOverride() {}

    public Integer getLeagueApiId() { return leagueApiId; }
    public void setLeagueApiId(Integer leagueApiId) { this.leagueApiId = leagueApiId; }

    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public Integer getPlayerApiId() { return playerApiId; }
    public void setPlayerApiId(Integer playerApiId) { this.playerApiId = playerApiId; }

    public Integer getApiClubId() { return apiClubId; }
    public void setApiClubId(Integer apiClubId) { this.apiClubId = apiClubId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

