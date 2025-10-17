/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// Player.java
// data/storage.impl.jpa/src/main/java/com/injurytime/storage/jpa/entity/Player.java
package com.injurytime.storage.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "player")
public class Player {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "PLAYER_ID")
  private Integer id;                        // surrogate PK

  @Column(name = "PLAYER_API_ID", nullable = false, unique = true)
  private Integer playerApiId;               // external ID

  @Column(name = "PLAYER_NAME", nullable = false)
  private String playerName;

  @Column(name = "DATE_OF_BIRTH")
  private java.time.LocalDate dateOfBirth;

  @Column(name = "HEIGHT_CM")
  private Short heightCm;

  @Column(name = "NATIONALITY_CODE", length = 3)
  private String nationalityCode;

  @Column(name = "IMAGE_URL")
  private String imageUrl;

  @Column(name = "CREATED_AT", insertable = false, updatable = false)
  private java.time.Instant createdAt;

  @Column(name = "UPDATED_AT", insertable = false, updatable = false)
  private java.time.Instant updatedAt;

  public Player() {}
  public Integer getId() { return id; }
  public Integer getPlayerApiId() { return playerApiId; }
  public void setPlayerApiId(Integer v) { this.playerApiId = v; }
  public String  getPlayerName()  { return playerName; }
  public void setPlayerName(String v) { this.playerName = v; }

    public LocalDate getDateOfBirth()
    {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth)
    {
        this.dateOfBirth = dateOfBirth;
    }

    public Short getHeightCm()
    {
        return heightCm;
    }

    public void setHeightCm(Short heightCm)
    {
        this.heightCm = heightCm;
    }

    public String getNationalityCode()
    {
        return nationalityCode;
    }

    public void setNationalityCode(String nationalityCode)
    {
        this.nationalityCode = nationalityCode;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public Instant getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt)
    {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt)
    {
        this.updatedAt = updatedAt;
    }
  
}



