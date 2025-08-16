/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.season.api;

import java.time.Instant;
import java.util.Optional;

/** Resolve the (league, season) pair for a given event/Article time. */
public interface SeasonResolver {

  /**
   * Resolve season, given article time and at least one hint (league or club).
   * @param articleInstant time of the article/event (nullable => use "current" season)
   * @param leagueApiId optional known league
   * @param apiClubId optional known club (used to infer league if league is null)
   * @return resolution with leagueApiId and season
   * @throws IllegalStateException if no season can be determined
   */
  SeasonResolution resolve(Optional<Instant> articleInstant,
                           Optional<Integer> leagueApiId,
                           Optional<Integer> apiClubId);

  /** Resolve the league's current season (IS_CURRENT=TRUE fallback). */
  SeasonResolution resolveCurrent(int leagueApiId);
}

