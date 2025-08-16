/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// LeagueSeasonId.java
package com.injurytime.storage.jpa.entity;

import java.io.Serializable;
import java.util.Objects;

public class LeagueSeasonId implements Serializable {
    private Integer leagueApiId;
    private Integer season;
    public LeagueSeasonId() {}
    public LeagueSeasonId(Integer leagueApiId, Integer season) {
        this.leagueApiId = leagueApiId; this.season = season;
    }
    @Override public boolean equals(Object o){ 
        if(this==o) return true; 
        if(!(o instanceof LeagueSeasonId)) return false;
        LeagueSeasonId that=(LeagueSeasonId)o;
        return Objects.equals(leagueApiId, that.leagueApiId) &&
               Objects.equals(season, that.season);
    }
    @Override public int hashCode(){ return Objects.hash(leagueApiId, season); }
}

