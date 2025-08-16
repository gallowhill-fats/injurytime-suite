/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// SquadRosterId.java
package com.injurytime.storage.jpa.entity;

import java.io.Serializable;
import java.util.Objects;

public class SquadRosterId implements Serializable {
    private Integer leagueApiId;
    private Integer season;
    private Integer apiClubId;
    private Integer playerApiId;
    public SquadRosterId() {}
    public SquadRosterId(Integer l, Integer s, Integer c, Integer p){
        leagueApiId=l; season=s; apiClubId=c; playerApiId=p;
    }
    @Override public boolean equals(Object o){
        if(this==o) return true;
        if(!(o instanceof SquadRosterId)) return false;
        SquadRosterId that=(SquadRosterId)o;
        return Objects.equals(leagueApiId, that.leagueApiId) &&
               Objects.equals(season, that.season) &&
               Objects.equals(apiClubId, that.apiClubId) &&
               Objects.equals(playerApiId, that.playerApiId);
    }
    @Override public int hashCode(){ return Objects.hash(leagueApiId, season, apiClubId, playerApiId); }
}

