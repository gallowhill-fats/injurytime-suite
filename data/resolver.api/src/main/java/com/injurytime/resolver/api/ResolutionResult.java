/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.resolver.api;

import java.util.List;

/**
 *
 * @author clayton
 */
public record ResolutionResult(String playerId, String playerName, String teamId,
                               double confidence, List<String> evidence) {}
