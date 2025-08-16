/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// ResolutionOverrideId.java
// data/storage.impl.jpa/src/main/java/com/injurytime/storage/jpa/entity/ResolutionOverrideId.java
package com.injurytime.storage.jpa.entity;

import java.io.Serializable;
import java.util.Objects;

public class ResolutionOverrideId implements Serializable {
  private Integer leagueApiId;
  private Integer season;
  private String rawText;

  public ResolutionOverrideId() {}
  public ResolutionOverrideId(Integer leagueApiId, Integer season, String rawText) {
    this.leagueApiId = leagueApiId; this.season = season; this.rawText = rawText;
  }
  @Override public boolean equals(Object o){
    if (this==o) return true;
    if (!(o instanceof ResolutionOverrideId)) return false;
    ResolutionOverrideId that=(ResolutionOverrideId)o;
    return Objects.equals(leagueApiId, that.leagueApiId)
        && Objects.equals(season, that.season)
        && Objects.equals(rawText, that.rawText);
  }
  @Override public int hashCode(){ return Objects.hash(leagueApiId, season, rawText); }
}


