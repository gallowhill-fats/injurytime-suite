/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

import java.util.List;

public record Dossier(
        long fixtureId,
        int leagueId, 
        int season,
        TeamBlock home, 
        TeamBlock away,
        List<ScoreBin> homeHist, 
        List<ScoreBin> awayHist, 
        List<ScoreBin> leagueHist,
        List<ScoreBin> homeHistAtHome, // when the HOME team played at HOME
        List<ScoreBin> homeHistAway, // when the HOME team played AWAY
        List<ScoreBin> awayHistAtHome, // when the AWAY team played at HOME
        List<ScoreBin> awayHistAway, // when the AWAY team played AWAY
        List<ScoreBin> leagueHistHome, // league, home-venue perspective
        List<ScoreBin> leagueHistAway, // league, away-venue perspective
        List<FormPoint> lastNPointsHome, 
        List<FormPoint> lastNPointsAway,
        List<MatchSummary> last5Home, 
        List<MatchSummary> last5Away
        ) {

}
