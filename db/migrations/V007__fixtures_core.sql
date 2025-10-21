-- V007__fixtures_core.sql

-- Master fixture row (one per match)
CREATE TABLE IF NOT EXISTS fixture (
  fixture_id       BIGINT PRIMARY KEY,          -- API: response[].fixture.id
  league_id        INT        NOT NULL,         -- API: league.id
  season           INT        NOT NULL,         -- API: league.season
  round_name       TEXT,
  match_date_utc   TIMESTAMPTZ NOT NULL,        -- API: fixture.date (UTC)
  venue_id         INT,
  venue_name       TEXT,
  venue_city       TEXT,
  referee          TEXT,
  status_short     TEXT,
  status_long      TEXT,
  elapsed_minutes  INT
);

-- Snapshot of teams & result (kept up-to-date via upsert)
CREATE TABLE IF NOT EXISTS fixture_teams (
  fixture_id     BIGINT PRIMARY KEY REFERENCES fixture(fixture_id) ON DELETE CASCADE,
  home_team_id   INT        NOT NULL,           -- API: teams.home.id
  home_team      TEXT       NOT NULL,
  away_team_id   INT        NOT NULL,           -- API: teams.away.id
  away_team      TEXT       NOT NULL,
  home_goals     INT,
  away_goals     INT,
  ht_home_goals  INT,
  ht_away_goals  INT,
  ft_home_goals  INT,
  ft_away_goals  INT
);

-- Handy indexes
CREATE INDEX IF NOT EXISTS idx_fixture_by_league_round ON fixture(league_id, season, round_name);
CREATE INDEX IF NOT EXISTS idx_fixture_by_date         ON fixture(match_date_utc);

