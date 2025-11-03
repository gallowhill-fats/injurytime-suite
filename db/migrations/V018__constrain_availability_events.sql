-- V018__constrain_availability_events.sql

-- 0) Normalizer: keep param name AS 'p' to avoid rename error
CREATE OR REPLACE FUNCTION norm_alias(p text)
RETURNS text
LANGUAGE sql
IMMUTABLE
RETURNS NULL ON NULL INPUT
AS $$
  SELECT regexp_replace(lower(trim(p)), '\s+', ' ', 'g')
$$;


-- 1) Guard: ensure no NULL player ids remain before setting NOT NULL
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM availability_events WHERE player_api_id IS NULL) THEN
    RAISE EXCEPTION
      'availability_events.player_api_id has NULL rows. Run the backfill (V017) first.';
  END IF;
END$$;

-- 2) Enforce NOT NULL on player_api_id
ALTER TABLE availability_events
  ALTER COLUMN player_api_id SET NOT NULL;

-- 3) Add FK to player, use NOT VALID first to avoid table scan during migration
ALTER TABLE availability_events
  ADD CONSTRAINT fk_av_events_player
  FOREIGN KEY (player_api_id) REFERENCES player(player_api_id)
  NOT VALID;

-- Then validate (can run online)
ALTER TABLE availability_events
  VALIDATE CONSTRAINT fk_av_events_player;

-- 4) Helpful indexes (idempotent & with correct column names / types)

-- Lookups by club/team
CREATE INDEX IF NOT EXISTS idx_av_events_team
  ON availability_events(api_club_id);

-- Lookups by player in events
CREATE INDEX IF NOT EXISTS idx_av_events_player
  ON availability_events(player_api_id);

-- Club+player on roster (often used together)
CREATE INDEX IF NOT EXISTS idx_roster_club_player
  ON squad_roster(api_club_id, player_api_id);

-- Normalized player name on player
CREATE INDEX IF NOT EXISTS idx_player_name_norm
  ON player( (norm_alias(player_name)) );

-- Normalized alias on player_alias (correct column name!)
CREATE INDEX IF NOT EXISTS idx_alias_norm
  ON player_alias( (norm_alias(alias)) );

