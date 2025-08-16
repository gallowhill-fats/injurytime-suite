/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.resolver.db.repo;

import com.injurytime.storage.jpa.entity.*;
import jakarta.persistence.*;
import org.openide.util.lookup.ServiceProvider;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

import java.util.Optional;


public class JpaResolverRepository implements ResolverRepository {

  private final EntityManagerFactory emf;

  public JpaResolverRepository() {
    this.emf = Persistence.createEntityManagerFactory("injurytimePU");
  }

  // ---------- Aliases ----------
  @Override
  public Optional<PlayerAlias> findPlayerAlias(String normalizedAlias) {
    try (var em = emf.createEntityManager()) {
      var q = em.createQuery(
              "SELECT pa FROM PlayerAlias pa WHERE pa.normalizedAlias = :n", PlayerAlias.class);
      q.setParameter("n", normalizedAlias);
      return q.getResultStream().findFirst();
    }
  }

  @Override
  public Optional<ClubAlias> findClubAlias(String normalizedAlias) {
    try (var em = emf.createEntityManager()) {
      var q = em.createQuery(
              "SELECT ca FROM ClubAlias ca WHERE ca.normalizedAlias = :n", ClubAlias.class);
      q.setParameter("n", normalizedAlias);
      return q.getResultStream().findFirst();
    }
  }

  // ---------- Canonical by external IDs ----------
  @Override
  public Optional<Player> findPlayerByApiId(int playerApiId) {
    try (var em = emf.createEntityManager()) {
      var q = em.createQuery(
              "SELECT p FROM Player p WHERE p.playerApiId = :id", Player.class);
      q.setParameter("id", playerApiId);
      return q.getResultStream().findFirst();
    }
  }

  @Override
  public Optional<Club> findClubByApiId(int apiClubId) {
    try (var em = emf.createEntityManager()) {
      var q = em.createQuery(
              "SELECT c FROM Club c WHERE c.apiClubId = :id", Club.class);
      q.setParameter("id", apiClubId);
      return q.getResultStream().findFirst();
    }
  }

  // ---------- Roster candidates ----------
  @Override
  public List<Player> findRosterPlayers(int leagueApiId, int season, int apiClubId) {
    try (var em = emf.createEntityManager()) {
      // Pull players via roster; JOIN FETCH to avoid N+1
      var q = em.createQuery(
        "SELECT DISTINCT p " +
        "FROM SquadRoster sr " +
        "JOIN sr.player p " +
        "WHERE sr.leagueApiId = :lid AND sr.season = :s AND sr.apiClubId = :cid",
        Player.class);
      q.setParameter("lid", leagueApiId);
      q.setParameter("s", season);
      q.setParameter("cid", apiClubId);
      return q.getResultList();
    }
  }

  // ---------- League/season helpers ----------
  @Override
  public List<Object[]> listClubSeasonsWithLeagueMeta(int apiClubId) {
    try (var em = emf.createEntityManager()) {
      return em.createQuery(
        "SELECT cs.leagueApiId, cs.season, ls.seasonStart, ls.seasonEnd, ls.isCurrent " +
        "FROM ClubSeason cs JOIN LeagueSeason ls " +
        "ON ls.leagueApiId = cs.leagueApiId AND ls.season = cs.season " +
        "WHERE cs.apiClubId = :club", Object[].class)
        .setParameter("club", apiClubId)
        .getResultList();
    }
  }

  @Override
  public Optional<LeagueSeason> currentLeagueSeason(int leagueApiId) {
    try (var em = emf.createEntityManager()) {
      return em.createQuery(
        "SELECT ls FROM LeagueSeason ls WHERE ls.leagueApiId = :lid AND ls.isCurrent = TRUE",
        LeagueSeason.class)
        .setParameter("lid", leagueApiId)
        .getResultStream().findFirst();
    }
  }

  // ---------- Overrides ----------
  @Override
public Optional<ResolutionOverride> findOverride(int leagueApiId, int season, String rawText) {
    EntityManager em = emf.createEntityManager();
    try {
        return Optional.ofNullable(
            em.find(ResolutionOverride.class, new ResolutionOverrideId(leagueApiId, season, rawText))
        );
    } finally {
        if (em.isOpen()) em.close();
    }
}

  // ---------- Domain hints ----------
  @Override
public Optional<ClubDomainHint> findFirstDomainHintForUrl(String url) {
  String hostAndPath = extractHostAndPath(url);
  if (hostAndPath == null || hostAndPath.isBlank()) return Optional.empty();

  EntityManager em = emf.createEntityManager();
  try {
    var hits = em.createQuery(
        "SELECT h FROM ClubDomainHint h " +
        "WHERE LOCATE(h.domainPattern, :haystack) > 0 " +
        "ORDER BY LENGTH(h.domainPattern) DESC",  // prefer more specific patterns
        ClubDomainHint.class)
      .setParameter("haystack", hostAndPath)
      .setMaxResults(1)
      .getResultList();
    return hits.stream().findFirst();
  } finally {
    if (em.isOpen()) em.close();
  }
}


  private static String extractHostAndPath(String url) {
    try {
      URI u = URI.create(url);
      String host = u.getHost() == null ? "" : u.getHost();
      String path = u.getPath() == null ? "" : u.getPath();
      return host + path;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  // ---------- Global fuzzy prefilter (LIKE) ----------
  @Override
  public List<Player> findPlayersByNameLike(String needleLower, int limit) {
    try (var em = emf.createEntityManager()) {
      var cb = em.getCriteriaBuilder();
      var cq = cb.createQuery(Player.class);
      var root = cq.from(Player.class);
      // LOWER(name) LIKE %needle%
      var like = cb.like(cb.lower(root.get("playerName")), "%" + needleLower + "%");
      cq.select(root).where(like);
      return em.createQuery(cq).setMaxResults(limit).getResultList();
    }
  }
  
  @Override
public List<Club> findClubsByNameLike(String needleLower, int limit) {
  try (var em = emf.createEntityManager()) {
    var cb = em.getCriteriaBuilder();
    var cq = cb.createQuery(Club.class);
    var root = cq.from(Club.class);
    var like = cb.like(cb.lower(root.get("clubName")), "%" + needleLower + "%");
    cq.select(root).where(like);
    return em.createQuery(cq).setMaxResults(limit).getResultList();
  }
}

}

