/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

public record TableRow(
        int rank, String teamName,
        int played, int won, int drawn, int lost,
        int gfHome, int gaHome, int gfAway, int gaAway,
        int goalDiff, int points, String form5
        ) {

}
