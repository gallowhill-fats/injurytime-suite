/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// Club.java
package com.injurytime.storage.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "CLUB")
@Access(AccessType.FIELD)
public class Club {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "CLUB_ID")
  private Integer id; // DB surrogate PK

  @Column(name = "API_CLUB_ID", nullable = false, unique = true)
  private Integer apiClubId; // canonical external ID

  @Column(name = "CLUB_NAME", nullable = false, unique = true, length = 100)
  private String clubName;

  @Column(name = "CLUB_ABBR", nullable = false, unique = true, length = 4)
  private String clubAbbr;

  public Club() {}

  public Integer getId() { return id; }

  public Integer getApiClubId() { return apiClubId; }
  public void setApiClubId(Integer v) { this.apiClubId = v; }

  public String getClubName() { return clubName; }
  public void setClubName(String v) { this.clubName = v; }

  public String getClubAbbr() { return clubAbbr; }
  public void setClubAbbr(String v) { this.clubAbbr = v; }
}


