/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.resolver.db;

import com.injurytime.resolver.db.repo.ResolverRepository;
import com.injurytime.resolver.db.util.NameNormalizer;
import com.injurytime.storage.jpa.entity.Player;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import java.util.Objects;
import com.injurytime.resolver.api.PlayerResolver;
import com.injurytime.resolver.api.ResolutionResult;
import com.injurytime.resolver.api.SeasonKey;
import com.injurytime.resolver.api.TeamHint;
import com.injurytime.resolver.api.TeamResolver;
import java.time.LocalDate;
import java.util.*;

@ServiceProvider(service = PlayerResolver.class, position = 10)
public final class DbPlayerResolver implements PlayerResolver {

  private final ResolverRepository repo;
  private final NameNormalizer norm = new NameNormalizer();
  private final JaroWinklerSimilarity jw = new JaroWinklerSimilarity();

  @Override
  public ResolutionResult resolvePlayer(String rawName, TeamHint teamHint, SeasonKey seasonHint) {
    String normalized = norm.normalize(rawName);

    // 0) overrides (if season + raw text)
    if (teamHint != null && seasonHint != null) {
      var ov = repo.findOverride(Integer.parseInt(seasonHint.leagueId()),
                                 Integer.parseInt(seasonHint.seasonId()),
                                 rawName);
      if (ov.isPresent()) {
        var o = ov.get();
        if (o.getPlayerApiId() != null) {
          var p = repo.findPlayerByApiId(o.getPlayerApiId()).orElse(null);
          if (p != null) return new ResolutionResult(String.valueOf(p.getPlayerApiId()), p.getPlayerName(),
                      teamHint.teamId(), 1.0, List.of("override"));
        }
      }
    }

    // 1) alias exact
    var alias = repo.findPlayerAlias(normalized);
    if (alias.isPresent()) {
      var p = repo.findPlayerByApiId(alias.get().getPlayerApiId()).orElse(null);
      if (p != null) {
        return new ResolutionResult(String.valueOf(p.getPlayerApiId()), p.getPlayerName(),
            teamHint != null ? teamHint.teamId() : null, 0.99, List.of("alias"));
      }
    }

    // 2) roster-constrained fuzzy
    List<Player> candidates = List.of();
    if (teamHint != null && seasonHint != null
        && isInt(seasonHint.leagueId()) && isInt(seasonHint.seasonId())
        && isInt(teamHint.teamId())) {
      int lid = Integer.parseInt(seasonHint.leagueId());
      int season = Integer.parseInt(seasonHint.seasonId());
      int clubId = Integer.parseInt(teamHint.teamId());
      candidates = repo.findRosterPlayers(lid, season, clubId);
    }

    Player best = null;
    double bestScore = 0.0;
    for (Player p : candidates) {
      double score = jw.apply(norm.normalize(p.getPlayerName()), normalized);
      if (score > bestScore) { bestScore = score; best = p; }
    }
    if (best != null && bestScore >= 0.90) {
      return new ResolutionResult(String.valueOf(best.getPlayerApiId()), best.getPlayerName(),
          teamHint != null ? teamHint.teamId() : null,
          bestScore, List.of("roster+jw=" + bestScore));
    }

    // 3) global LIKE prefilter + fuzzy (fallback)
    var global = repo.findPlayersByNameLike(rawName.toLowerCase(Locale.ROOT), 50);
    best = null; bestScore = 0.0;
    for (Player p : global) {
      double score = jw.apply(norm.normalize(p.getPlayerName()), normalized);
      if (score > bestScore) { bestScore = score; best = p; }
    }
    if (best != null && bestScore >= 0.93) {
      return new ResolutionResult(String.valueOf(best.getPlayerApiId()), best.getPlayerName(),
          teamHint != null ? teamHint.teamId() : null,
          0.60 + 0.40 * bestScore, List.of("global+jw=" + bestScore));
    }

    return new ResolutionResult(null, rawName,
        teamHint != null ? teamHint.teamId() : null,
        0.0, List.of("no-match"));
  }

  private static boolean isInt(String s) {
    if (s == null) return false;
    try { Integer.parseInt(s); return true; } catch (NumberFormatException e) { return false; }
  }
  
  // DbPlayerResolver.java  (additions)


// constructors
public DbPlayerResolver(ResolverRepository repo) {
  this.repo = java.util.Objects.requireNonNull(repo);
}
public DbPlayerResolver() {
  this(org.openide.util.Lookup.getDefault().lookup(ResolverRepository.class));
}
}

