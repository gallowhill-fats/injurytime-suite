/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

public interface LeagueTableService {

    java.util.List<TableRow> loadTable(int leagueId, int season, Integer weekOrNull);
}
