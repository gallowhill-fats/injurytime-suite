-- V000__master_core.sql
SET search_path TO public;

-- ======== CLUB (master) ========
CREATE TABLE IF NOT EXISTS club (
  club_id        BIGSERIAL PRIMARY KEY,                     -- surrogate PK
  api_club_id    INTEGER  NOT NULL UNIQUE,                  -- canonical external ID
  club_name      TEXT     NOT NULL UNIQUE,
  club_abbr      CHAR(3)  NOT NULL UNIQUE,                  -- 3-letter code (change to CHAR(4) if needed)
  country_code   CHAR(3),                                   -- ISO-3166 alpha-3 (optional)
  logo_url       TEXT,                                      -- club logo image URL
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_club_code_format    CHECK (club_abbr ~ '^[A-Z0-9]{3}$'),
  CONSTRAINT ck_country_code_format CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{3}$')
);
CREATE INDEX IF NOT EXISTS idx_club_name ON club (club_name);

CREATE TABLE IF NOT EXISTS club_alias (
  api_club_id  INTEGER NOT NULL REFERENCES club(api_club_id) ON DELETE CASCADE,
  alias        TEXT    NOT NULL,
  PRIMARY KEY (api_club_id, alias)
);

CREATE TABLE IF NOT EXISTS club_domain_hint (
  api_club_id  INTEGER NOT NULL REFERENCES club(api_club_id) ON DELETE CASCADE,
  domain       TEXT    NOT NULL,
  confidence   INTEGER NOT NULL CHECK (confidence BETWEEN 0 AND 100),
  PRIMARY KEY (api_club_id, domain)
);

-- ======== PLAYER (master) ========
CREATE TABLE IF NOT EXISTS player (
  player_id        BIGSERIAL PRIMARY KEY,                   -- surrogate PK
  player_api_id    INTEGER  NOT NULL UNIQUE,                -- canonical external ID
  player_name      TEXT     NOT NULL,
  date_of_birth    DATE,
  height_cm        SMALLINT,                                -- sanity range below
  nationality_code CHAR(3),
  image_url        TEXT,                                    -- player image URL
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_height_cm_range  CHECK (height_cm IS NULL OR height_cm BETWEEN 120 AND 230),
  CONSTRAINT ck_nat_code_format  CHECK (nationality_code IS NULL OR nationality_code ~ '^[A-Z]{3}$')
);
CREATE INDEX IF NOT EXISTS idx_player_name ON player (player_name);

CREATE TABLE IF NOT EXISTS player_alias (
  player_api_id  INTEGER NOT NULL REFERENCES player(player_api_id) ON DELETE CASCADE,
  alias          TEXT    NOT NULL,
  PRIMARY KEY (player_api_id, alias)
);

-- ======== SQUAD_ROSTER ========
CREATE TABLE IF NOT EXISTS squad_roster (
  season_id       TEXT    NOT NULL,
  api_club_id     INTEGER NOT NULL REFERENCES club(api_club_id) ON DELETE CASCADE,
  player_api_id   INTEGER NOT NULL REFERENCES player(player_api_id) ON DELETE CASCADE,
  position_code   TEXT,
  shirt_number    INTEGER,
  on_loan         BOOLEAN,
  loan_from_club  INTEGER REFERENCES club(api_club_id),
  join_date       DATE,
  leave_date      DATE,
  updated         TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT pk_squad_roster PRIMARY KEY (season_id, api_club_id, player_api_id)
);
CREATE INDEX IF NOT EXISTS idx_roster_club   ON squad_roster (api_club_id);
CREATE INDEX IF NOT EXISTS idx_roster_player ON squad_roster (player_api_id);
CREATE INDEX IF NOT EXISTS idx_roster_season ON squad_roster (season_id);

