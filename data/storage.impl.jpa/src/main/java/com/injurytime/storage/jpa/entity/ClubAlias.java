/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// ClubAlias.java
// data/storage.impl.jpa/src/main/java/com/injurytime/storage/jpa/entity/ClubAlias.java
package com.injurytime.storage.jpa.entity;

import jakarta.persistence.*;

@Entity @Table(name="CLUB_ALIAS") @Access(AccessType.FIELD)
public class ClubAlias {
  @Id @Column(name="ALIAS", length=200) private String alias;
  @Column(name="API_CLUB_ID", nullable=false) private Integer apiClubId;
  @Column(name="NORMALIZED_ALIAS", nullable=false, length=200) private String normalizedAlias;

  @ManyToOne(fetch=FetchType.LAZY)
  @JoinColumn(name="API_CLUB_ID", referencedColumnName="API_CLUB_ID", insertable=false, updatable=false)
  private Club club;

  public ClubAlias() {}
  public String getAlias() { return alias; }
  public void setAlias(String v) { this.alias = v; }
  public Integer getApiClubId() { return apiClubId; }             // <-- needed
  public void setApiClubId(Integer v) { this.apiClubId = v; }
  public String getNormalizedAlias() { return normalizedAlias; }
  public void setNormalizedAlias(String v) { this.normalizedAlias = v; }
  public Club getClub() { return club; }
  public void setClub(Club v) { this.club = v; }
}


