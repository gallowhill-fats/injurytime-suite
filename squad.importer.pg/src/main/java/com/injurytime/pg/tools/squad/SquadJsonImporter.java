/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// SquadJsonImporter.java
package com.injurytime.pg.tools.squad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.injurytime.storage.api.JpaAccess;
import com.injurytime.storage.jpa.entity.SquadRoster;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.util.Map.entry;
import com.injurytime.storage.api.AppConfig;
import java.util.Map;
import java.util.logging.Logger;

public final class SquadJsonImporter implements AutoCloseable {

    private final JpaAccess jpa;
    private final ObjectMapper mapper = new ObjectMapper();
    // Only present when using the built-in PU wiring (withPostgresPU); null if caller supplies JpaAccess.
    private final EntityManagerFactory emf;

    // ✔ Existing constructor (kept): caller supplies a JpaAccess (e.g., your platform-wide JPA service)
    public SquadJsonImporter(JpaAccess jpa)
    {
        if (jpa == null)
        {
            throw new IllegalArgumentException("JpaAccess must not be null");
        }
        this.jpa = jpa;
        this.emf = null;
    }

    /**
     * ✔ New convenience factory: build a JpaAccess backed by the Postgres
     * persistence-unit "injurytime-pg". The PU must exist in this module at:
     * src/main/resources/META-INF/persistence.xml
     */
    public static SquadJsonImporter withPostgresPU()
    {

        var overrides = Map.ofEntries(
                entry("jakarta.persistence.jdbc.url", AppConfig.get("PG_URL")),
                entry("jakarta.persistence.jdbc.user", AppConfig.get("PG_USER")),
                entry("jakarta.persistence.jdbc.password", AppConfig.get("PG_PASS"))
        );

        Logger.getAnonymousLogger().info("PG_URL=" + AppConfig.get("PG_URL"));
        Logger.getAnonymousLogger().info("PG_USER=" + AppConfig.get("PG_USER"));
        Logger.getAnonymousLogger().info("PG_PASS set? " + (AppConfig.get("PG_PASS") != null));

        var emf = Persistence.createEntityManagerFactory("injurytime-pg", overrides);
        // Minimal JpaAccess wrapper using the EMF above.
        JpaAccess wrapped = new JpaAccess() {
            @Override
            public <T> T tx(java.util.function.Function<EntityManager, T> work)
            {
                var em = emf.createEntityManager();
                try
                {
                    em.getTransaction().begin();
                    T out = work.apply(em);
                    em.getTransaction().commit();
                    return out;
                } catch (RuntimeException ex)
                {
                    if (em.getTransaction().isActive())
                    {
                        em.getTransaction().rollback();
                    }
                    throw ex;
                } finally
                {
                    em.close();
                }
            }
        };
        return new SquadJsonImporter(wrapped, emf);
    }

    // Private ctor used by the factory above.
    private SquadJsonImporter(JpaAccess jpa, EntityManagerFactory emf)
    {
        this.jpa = jpa;
        this.emf = emf;
    }

    // --- result carrier for the toast/UI ---
    public record ImportResult(int inserted, int updated, int skipped) {

    }

    /**
     * Import a single team squad JSON into SQUAD_ROSTER for the given season.
     */
    public ImportResult importFile(Path jsonFile, String seasonId) throws IOException
    {
        String json = Files.readString(jsonFile);
        JsonNode root = mapper.readTree(json);

        JsonNode resp = root.path("response");
        if (!resp.isArray() || resp.isEmpty())
        {
            return new ImportResult(0, 0, 0);
        }

        JsonNode block = resp.get(0);
        int apiClubId = block.path("team").path("id").asInt();
        String clubName = block.path("team").path("name").asText(null);
        String clubLogo = block.path("team").path("logo").asText(null);
            
        JsonNode players = block.path("players");
        if (!players.isArray())
        {
            return new ImportResult(0, 0, 0);
        }

        final int[] inserted =
        {
            0
        };
        final int[] updated =
        {
            0
        };
        final int[] skipped =
        {
            0
        };

        jpa.tx(em ->
        {
            ensureClubExistsAndUpdateLogo(em, apiClubId, clubName, clubLogo);

            for (JsonNode p : players)
            {
                String playerName = p.path("name").asText(null);
                int playerApiId = p.path("id").asInt();
                String posHuman = p.path("position").asText(null);
                String posCode = mapPosition(posHuman);
                Integer shirtNo = p.hasNonNull("number") ? p.path("number").asInt() : null;

                // Optional fields – wire up if you start getting them from the JSON
                Boolean onLoan = null;
                Integer loanFromClub = null;   // API club id of parent if on loan
                java.time.LocalDate joinDate = null;
                java.time.LocalDate leaveDate = null;
                java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now();

                String imageUrl = p.path("photo").asText(null);
                upsertPlayer(em, playerApiId, playerName, imageUrl);

                // find-or-create by (SEASON_ID, API_CLUB_ID, PLAYER_API_ID)
                SquadRoster roster = em.createQuery(
                        "SELECT r FROM SquadRoster r "
                        + "WHERE r.seasonId = :sid AND r.apiClubId = :cid AND r.playerApiId = :pid",
                        SquadRoster.class)
                        .setParameter("sid", seasonId)
                        .setParameter("cid", apiClubId)
                        .setParameter("pid", playerApiId)
                        .getResultStream().findFirst().orElse(null);

                if (roster == null)
                {
                    roster = new SquadRoster();
                    roster.setSeasonId(seasonId);
                    roster.setApiClubId(apiClubId);
                    roster.setPlayerApiId(playerApiId);
                    roster.setPositionCode(posCode);
                    roster.setShirtNumber(shirtNo);
                    roster.setOnLoan(onLoan);
                    roster.setLoanFromClub(loanFromClub);
                    roster.setJoinDate(joinDate);
                    roster.setLeaveDate(leaveDate);
                    roster.setUpdated(updatedAt);
                    em.persist(roster);
                    inserted[0]++;
                } else
                {
                    boolean changed = false;
                    changed |= setIfDiff(roster.getPositionCode(), posCode, roster::setPositionCode);
                    changed |= setIfDiff(roster.getShirtNumber(), shirtNo, roster::setShirtNumber);
                    changed |= setIfDiff(roster.getOnLoan(), onLoan, roster::setOnLoan);
                    changed |= setIfDiff(roster.getLoanFromClub(), loanFromClub, roster::setLoanFromClub);
                    changed |= setIfDiff(roster.getJoinDate(), joinDate, roster::setJoinDate);
                    changed |= setIfDiff(roster.getLeaveDate(), leaveDate, roster::setLeaveDate);
                    changed |= setIfDiff(roster.getUpdated(), updatedAt, roster::setUpdated);

                    if (changed)
                    {
                        updated[0]++;
                    } else
                    {
                        skipped[0]++;
                    }
                }
            }
            return null;
        });

        return new ImportResult(inserted[0], updated[0], skipped[0]);
    }

    // Insert the player if missing. No schema is hard-coded; we rely on the connection's default schema.
    private static void upsertPlayer(EntityManager em, int playerApiId, String playerName, String imageUrl)
    {
        // Look up by the unique natural key (PLAYER_API_ID), not the @Id
        var existing = em.createQuery(
                "SELECT p FROM Player p WHERE p.playerApiId = :pid", com.injurytime.storage.jpa.entity.Player.class)
                .setParameter("pid", playerApiId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (existing == null)
        {
            var p = new com.injurytime.storage.jpa.entity.Player();
            p.setPlayerApiId(playerApiId);
            p.setPlayerName(playerName);
            p.setImageUrl(imageUrl);          // <- writes IMAGE_URL
            em.persist(p);
        } else
        {
            boolean changed = false;
            if (!java.util.Objects.equals(existing.getPlayerName(), playerName))
            {
                existing.setPlayerName(playerName);
                changed = true;
            }
            if (!java.util.Objects.equals(existing.getImageUrl(), imageUrl))
            {
                existing.setImageUrl(imageUrl);
                changed = true;
            }
            // managed entity -> auto-flushed at TX end
        }
    }

    private static String mapPosition(String human)
    {
        if (human == null)
        {
            return null;
        }
        switch (human.toLowerCase())
        {
            case "goalkeeper":
                return "GK";
            case "defender":
                return "DF";
            case "midfielder":
                return "MF";
            case "attacker":
            case "forward":
                return "FW";
            default:
                return human;
        }
    }

    /**
     * Overload used by the action; delegates to the Path method.
     */
    public ImportResult importFile(java.io.File jsonFile, String seasonId, int clubApiId) throws Exception
    {
        if (jsonFile == null)
        {
            throw new IllegalArgumentException("jsonFile must not be null");
        }
        return importFile(jsonFile.toPath(), seasonId);
    }

    // small utility to track “updated vs skipped”
    private static <T> boolean setIfDiff(T cur, T val, java.util.function.Consumer<T> setter)
    {
        if (!java.util.Objects.equals(cur, val))
        {
            setter.accept(val);
            return true;
        }
        return false;
    }

    /**
     * Close EMF if this importer created it via withPostgresPU(). Safe to call
     * otherwise.
     */
    @Override
    public void close()
    {
        if (emf != null && emf.isOpen())
        {
            emf.close();
        }
    }

    private static void ensureClubExistsAndUpdateLogo(EntityManager em, int apiClubId, String clubName, String logoUrl)
    {
        var club = em.createQuery(
                "SELECT c FROM Club c WHERE c.apiClubId = :id", com.injurytime.storage.jpa.entity.Club.class)
                .setParameter("id", apiClubId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (club == null)
        {
            throw new IllegalStateException("Club api_club_id=" + apiClubId + " not found. Seed clubs CSV first.");
        }
        boolean changed = false;
        if (clubName != null && !clubName.equals(club.getClubName()))
        {
            club.setClubName(clubName);
            changed = true;
        }
        if (logoUrl != null && !java.util.Objects.equals(club.getLogoUrl(), logoUrl))
        {
            club.setLogoUrl(logoUrl);
            changed = true;
        }
    }

}
