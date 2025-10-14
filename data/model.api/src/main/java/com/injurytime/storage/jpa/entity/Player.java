/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// Player.java
// data/storage.impl.jpa/src/main/java/com/injurytime/storage/jpa/entity/Player.java
package com.injurytime.storage.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name="PLAYER") @Access(AccessType.FIELD)
public class Player {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name="PLAYER_ID") private Integer id;

  @Column(name="PLAYER_API_ID", nullable=false, unique=true) private Integer playerApiId;
  @Column(name="PLAYER_NAME",   nullable=false, length=100)  private String  playerName;
  @Column(name="DOB")    private LocalDate dob;
  @Column(name="HEIGHT") private BigDecimal height;

  public Player() {}
  public Integer getId() { return id; }
  public Integer getPlayerApiId() { return playerApiId; }
  public void setPlayerApiId(Integer v) { this.playerApiId = v; }
  public String  getPlayerName()  { return playerName; }
  public void setPlayerName(String v) { this.playerName = v; }
  public LocalDate getDob() { return dob; }
  public void setDob(LocalDate v) { this.dob = v; }
  public BigDecimal getHeight() { return height; }
  public void setHeight(BigDecimal v) { this.height = v; }
}



