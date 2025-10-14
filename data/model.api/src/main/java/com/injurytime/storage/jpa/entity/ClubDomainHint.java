/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// ClubDomainHint.java
// data/storage.impl.jpa/src/main/java/com/injurytime/storage/jpa/entity/ClubDomainHint.java
package com.injurytime.storage.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "CLUB_DOMAIN_HINT")
@Access(AccessType.FIELD)
public class ClubDomainHint {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Long id;

  @Column(name = "API_CLUB_ID", nullable = false)
  private Integer apiClubId;

  @Column(name = "DOMAIN_PATTERN", nullable = false, length = 200)
  private String domainPattern;

  @Column(name = "NOTES", length = 200)
  private String notes;

  public ClubDomainHint() {}

  public Long getId() { return id; }

  public Integer getApiClubId() { return apiClubId; }
  public void setApiClubId(Integer v) { this.apiClubId = v; }

  public String getDomainPattern() { return domainPattern; }   // <-- needed by your code
  public void setDomainPattern(String v) { this.domainPattern = v; }

  public String getNotes() { return notes; }
  public void setNotes(String v) { this.notes = v; }
}


