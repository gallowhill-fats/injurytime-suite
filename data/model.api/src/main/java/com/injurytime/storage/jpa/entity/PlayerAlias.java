/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// PlayerAlias.java
package com.injurytime.storage.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "PLAYER_ALIAS")
@Access(AccessType.FIELD)
public class PlayerAlias {
    @Id
    @Column(name = "ALIAS", length = 200)
    private String alias;

    @Column(name = "PLAYER_API_ID", nullable = false)
    private Integer playerApiId;

    @Column(name = "NORMALIZED_ALIAS", nullable = false, length = 200)
    private String normalizedAlias;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLAYER_API_ID", referencedColumnName = "PLAYER_API_ID", insertable = false, updatable = false)
    private Player player;

    public PlayerAlias() {}

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public Integer getPlayerApiId() { return playerApiId; }
    public void setPlayerApiId(Integer playerApiId) { this.playerApiId = playerApiId; }

    public String getNormalizedAlias() { return normalizedAlias; }
    public void setNormalizedAlias(String normalizedAlias) { this.normalizedAlias = normalizedAlias; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    
}


