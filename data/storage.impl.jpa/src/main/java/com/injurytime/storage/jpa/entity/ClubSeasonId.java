/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// ClubSeasonId.java
package com.injurytime.storage.jpa.entity;

import java.io.Serializable;
import java.util.Objects;

public class ClubSeasonId implements Serializable {
    private Integer leagueApiId;
    private Integer season;
    private Integer apiClubId;
    public ClubSeasonId() {}
    public ClubSeasonId(Integer l, Integer s, Integer c){ leagueApiId=l; season=s; apiClubId=c; }
    @Override public boolean equals(Object o){
        if(this==o) return true;
        if(!(o instanceof ClubSeasonId)) return false;
        ClubSeasonId that=(ClubSeasonId)o;
        return Objects.equals(leagueApiId, that.leagueApiId) &&
               Objects.equals(season, that.season) &&
               Objects.equals(apiClubId, that.apiClubId);
    }
    @Override public int hashCode(){ return Objects.hash(leagueApiId, season, apiClubId); }
}

