/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// SquadRosterId.java
package com.injurytime.storage.jpa.entity;

import java.io.Serializable;
import java.util.Objects;

public class SquadRosterId implements Serializable {
  private String seasonId;
  private Integer apiClubId;
  private Integer playerApiId;

  public SquadRosterId() {}
  public SquadRosterId(String seasonId, Integer apiClubId, Integer playerApiId) {
    this.seasonId = seasonId;
    this.apiClubId = apiClubId;
    this.playerApiId = playerApiId;
  }

  public String getSeasonId() { return seasonId; }
  public void setSeasonId(String seasonId) { this.seasonId = seasonId; }

  public Integer getApiClubId() { return apiClubId; }
  public void setApiClubId(Integer apiClubId) { this.apiClubId = apiClubId; }

  public Integer getPlayerApiId() { return playerApiId; }
  public void setPlayerApiId(Integer playerApiId) { this.playerApiId = playerApiId; }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SquadRosterId)) return false;
    SquadRosterId that = (SquadRosterId) o;
    return Objects.equals(seasonId, that.seasonId)
        && Objects.equals(apiClubId, that.apiClubId)
        && Objects.equals(playerApiId, that.playerApiId);
  }
  @Override public int hashCode() { return Objects.hash(seasonId, apiClubId, playerApiId); }
}
