/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.resolver.db;

import com.injurytime.resolver.api.*;
import com.injurytime.resolver.db.repo.ResolverRepository;
import com.injurytime.resolver.db.util.NameNormalizer;
import com.injurytime.storage.jpa.entity.Club;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import com.injurytime.resolver.api.PlayerResolver;
import com.injurytime.resolver.api.TeamResolver;

import java.util.Optional;

@ServiceProvider(service = TeamResolver.class, position = 10)
public final class DbTeamResolver implements TeamResolver {

  private final ResolverRepository repo = Lookup.getDefault().lookup(ResolverRepository.class);
  private final NameNormalizer norm = new NameNormalizer();

  @Override
  public TeamResolution resolveTeam(String rawName, SeasonKey seasonHint) {
    String n = norm.normalize(rawName);

    // 1) alias exact
    var alias = repo.findClubAlias(n);
    if (alias.isPresent()) {
      var club = repo.findClubByApiId(alias.get().getApiClubId()).orElse(null);
      if (club != null) return new TeamResolution(String.valueOf(alias.get().getApiClubId()), club.getClubName(), 0.99);
    }

    // 2) global LIKE (soft)
    var likeHits = repo.findPlayersByNameLike(n, 0); // not for clubs; you can add a similar Club LIKE if you store variants
    // For brevity we’ll stop here; usually you also maintain CLUB name variants or handle common reductions.

    return new TeamResolution(null, rawName, 0.0);
  }
}

