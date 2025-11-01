InjuryTime Suite

NetBeans Platform application for ingesting football player availability (injuries, suspensions, etc.).
Current milestone focuses on PostgreSQL ingest of squads and media (player photos, club logos), laying the groundwork for Gmail Alerts parsing.

✨ What’s in this repo

PostgreSQL schema (Flyway): clubs, players, squad rosters, availability events.

Squad importer (Pg): imports squad JSON files, writes players + roster, updates club logos.

Bulk squad importer  (API-Football -> PG) from csv of team ids.

Bulk fixture importer (API-Football -> PG) from csv of league ids.

Bulk fixture event importer (API-Football -> PG) from list of completed fixture ids.

Seeds: clubs can be preloaded from CSV (with supplied canonical 3-letter codes).

🧭 Architecture (modules)

data/model.api – shared models & JPA entities (e.g., Player, Club, SquadRoster, InjuryFact).

data/storage.api – storage interfaces (e.g., JpaAccess).

data/storage.impl.jpa – JPA/Hibernate implementation, DB wiring.

squad.importer.pg – NetBeans action: Tools → Import Squad JSON (Pg).

ingest.api – houses Flyway plugin & DB migrations.

resolver.impl.db – DB-backed resolution helpers (internal).

db/migrations – Flyway SQL migrations.

Packaging is NetBeans NBM; build via Maven.

✅ Prerequisites

Java 21

Maven 3.9+

PostgreSQL 14+ (local)

Access to squad JSON files (API-Football format)

🛠️ Local setup
1) Create database & user

-- In psql as a superuser (e.g., postgres)
CREATE DATABASE injurytime;
CREATE USER injurytime_app WITH PASSWORD 'change-me';
GRANT ALL PRIVILEGES ON DATABASE injurytime TO injurytime_app;

-- Optional: default schema
ALTER ROLE injurytime_app IN DATABASE injurytime SET search_path = public;

2) App configuration

Create injurytime.local.properties under your user home (NetBeans Platform reads userdir),
or put it on the application classpath (e.g., in your run configuration).

# JDBC (PostgreSQL)
db.driver=org.postgresql.Driver
db.url=jdbc:postgresql://localhost:5432/injurytime
db.user=injurytime_app
db.password=change-me

# Hibernate options (resource local)
hibernate.hbm2ddl.auto=none
hibernate.show_sql=false
hibernate.format_sql=true

The PG persistence unit name is injurytime-pg.

3) Apply migrations (Flyway)

From the repo root:

mvn -pl ingest.api -DskipTests flyway:info
mvn -pl ingest.api -DskipTests flyway:migrate \
  -Dflyway.url="jdbc:postgresql://localhost:5432/injurytime" \
  -Dflyway.user="injurytime_app" \
  -Dflyway.password="change-me" \
  -Dflyway.locations="filesystem:db/migrations" \
  -Dflyway.defaultSchema=public -Dflyway.schemas=public

You should see tables like: club, player, squad_roster,
and availability tables: availability_events_raw, availability_events, etc.

4) Seed clubs from CSV (once)

If you have clubs.csv with canonical IDs + 3-letter codes:

-- In psql as injurytime_app
CREATE TEMP TABLE staging_club(
  club_id BIGINT,          -- ignored
  api_club_id INT,
  club_name TEXT,
  club_abbr TEXT
);

\copy staging_club (club_id, api_club_id, club_name, club_abbr) \
  FROM 'D:/path/to/clubs.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');

INSERT INTO club (api_club_id, club_name, club_abbr)
SELECT api_club_id, NULLIF(btrim(club_name), ''), UPPER(NULLIF(btrim(club_abbr), ''))
FROM staging_club
WHERE api_club_id IS NOT NULL
ON CONFLICT (api_club_id) DO UPDATE
  SET club_name = EXCLUDED.club_name,
      club_abbr = EXCLUDED.club_abbr;

This ensures FK integrity for roster imports. Logos will be populated during squad imports.

▶️ Build & Run

Full build

mvn clean install

Run the NetBeans Platform app (module depends on your app launcher module)
Typically you’ll run the generated application from your IDE or by executing the app module.

In the running app:

Go to Tools → Import Squad JSON (Pg)

Choose a squad JSON file (API-Football format).

On success:

player rows are upserted (name, image_url).

club.logo_url is updated from the JSON’s team.logo.

squad_roster row is inserted/updated for that season.

🔍 Quick sanity queries

-- clubs with logos
SELECT api_club_id, club_name, club_abbr, logo_url
FROM club WHERE logo_url IS NOT NULL ORDER BY api_club_id LIMIT 10;

-- players with photos from the imported roster
SELECT p.player_api_id, p.player_name, p.image_url
FROM player p
JOIN squad_roster r ON r.player_api_id = p.player_api_id
ORDER BY p.player_api_id LIMIT 10;

🧩 Notes / conventions

We standardize on:

player_api_id for players

api_club_id for clubs (kept as-is for now)

club_abbr is the canonical 3-letter code (seeded via CSV, not generated).

Triggers:

club.updated_at, player.updated_at auto-touch on update.

squad_roster.updated auto-touch on update (separate trigger).

🧯 Troubleshooting

FK violation inserting squad_roster
Seed club entries first (CSV). Importer updates logos; it doesn’t create clubs.

No Persistence provider for EntityManager named injurytime-pg
Ensure persistence.xml is on module classpath, Hibernate provider present.

org.hibernate.proxy.HibernateProxy not found
Ensure Hibernate + runtime deps are packaged into the NetBeans module (we use a libs.hibernate.core wrapper).

CRLF line-ending warnings
Add .gitattributes:

* text=auto
*.sql text eol=lf
*.properties text eol=lf
*.xml text eol=lf
*.java text eol=lf
*.md text eol=lf

🗺️ Roadmap (next)

Gathering internet information sources.

Populating player alias database.

Integration with player rating system.

Fixture Dossier writer: pulls together background info on a set of weekly fixtures.

This includes:

* goal stats - clean sheets, over/under, BTTS
* graph of recent form
* summaries of recent fixtures
* top scorers
* injuries/suspensions
* score histograms

📜 License

Apache-2.0
