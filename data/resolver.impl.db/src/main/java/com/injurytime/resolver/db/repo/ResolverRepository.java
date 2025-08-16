/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.resolver.db.repo;

import com.injurytime.storage.jpa.entity.*;
import java.time.LocalDate;
import java.util.*;

public interface ResolverRepository {

  // --- Aliases (exact/normalized) ---
  Optional<PlayerAlias>  findPlayerAlias(String normalizedAlias);
  Optional<ClubAlias>    findClubAlias(String normalizedAlias);

  // --- Canonical by external IDs ---
  Optional<Player> findPlayerByApiId(int playerApiId);
  Optional<Club>   findClubByApiId(int apiClubId);

  // --- Roster candidates for resolver ---
  List<Player> findRosterPlayers(int leagueApiId, int season, int apiClubId);

  // --- League/season helpers ---
  List<Object[]> listClubSeasonsWithLeagueMeta(int apiClubId);
  Optional<LeagueSeason> currentLeagueSeason(int leagueApiId);

  // --- Overrides (manual disambiguations) ---
  Optional<ResolutionOverride> findOverride(int leagueApiId, int season, String rawText);

  // --- Domain hints (URL -> club) ---
  Optional<ClubDomainHint> findFirstDomainHintForUrl(String url);

  // --- Global fuzzy prefilter (name LIKE) ---
  List<Player> findPlayersByNameLike(String needleLower, int limit);
  
  List<Club> findClubsByNameLike(String needleLower, int limit);

}

