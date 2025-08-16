/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.resolver.api;

/**
 *
 * @author clayton
 */
public interface PlayerResolver {
  ResolutionResult resolvePlayer(String rawName, TeamHint teamHint, SeasonKey seasonHint);
}
