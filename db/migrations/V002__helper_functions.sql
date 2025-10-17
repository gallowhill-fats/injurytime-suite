-- V002__helper_functions.sql

CREATE EXTENSION IF NOT EXISTS pgcrypto;
SET search_path TO public;

-- =============== updated_at auto-touch ===============

CREATE OR REPLACE FUNCTION touch_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END$$;

-- add triggers where a column updated_at exists
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_schema='public' AND table_name='club' AND column_name='updated_at')
  THEN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_club_touch_updated_at') THEN
      CREATE TRIGGER trg_club_touch_updated_at
      BEFORE UPDATE ON club
      FOR EACH ROW
      EXECUTE FUNCTION touch_updated_at();
    END IF;
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_schema='public' AND table_name='player' AND column_name='updated_at')
  THEN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_player_touch_updated_at') THEN
      CREATE TRIGGER trg_player_touch_updated_at
      BEFORE UPDATE ON player
      FOR EACH ROW
      EXECUTE FUNCTION touch_updated_at();
    END IF;
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_schema='public' AND table_name='squad_roster' AND column_name='updated')
  THEN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_roster_touch_updated') THEN
      CREATE TRIGGER trg_roster_touch_updated
      BEFORE UPDATE ON squad_roster
      FOR EACH ROW
      EXECUTE FUNCTION touch_updated_at();
    END IF;
  END IF;
END$$;

-- =============== normalizers ===============

CREATE OR REPLACE FUNCTION normalize_status(p_status TEXT)
RETURNS TEXT
LANGUAGE plpgsql IMMUTABLE
AS $$
DECLARE s TEXT := lower(coalesce(trim(p_status), ''));
BEGIN
  IF s IN ('out','ruled out','injured') THEN RETURN 'out'; END IF;
  IF s IN ('doubtful','questionable')   THEN RETURN 'doubtful'; END IF;
  IF s IN ('probable','likely')         THEN RETURN 'probable'; END IF;
  IF s IN ('available','fit')           THEN RETURN 'available'; END IF;
  RETURN 'unknown';
END$$;

CREATE OR REPLACE FUNCTION normalize_availability_type(p_type TEXT)
RETURNS TEXT
LANGUAGE plpgsql IMMUTABLE
AS $$
DECLARE t TEXT := lower(coalesce(trim(p_type), ''));
BEGIN
  IF t IN ('injury','injured')             THEN RETURN 'injury'; END IF;
  IF t IN ('suspension','suspended','ban') THEN RETURN 'suspension'; END IF;
  IF t IN ('illness','sick')               THEN RETURN 'illness'; END IF;
  IF t IN ('rest','rotation')              THEN RETURN 'rest'; END IF;
  RETURN 'unknown';
END$$;

-- =============== claim hash helper ===============
-- stable hash from relevant parts so we can de-duplicate claims
CREATE OR REPLACE FUNCTION mk_claim_hash(
  p_source_system TEXT,
  p_source_uri    TEXT,
  p_player_api_id INT,
  p_api_club_id   INT,
  p_status        TEXT,
  p_type          TEXT,
  p_start_date    DATE
) RETURNS TEXT
LANGUAGE sql IMMUTABLE
AS $$
  SELECT encode(
           digest(
             convert_to(
               coalesce(p_source_system,'') || '|' ||
               coalesce(p_source_uri,'')    || '|' ||
               coalesce(p_player_api_id::text,'') || '|' ||
               coalesce(p_api_club_id::text,'')   || '|' ||
               coalesce(lower(p_status),'') || '|' ||
               coalesce(lower(p_type),'')   || '|' ||
               coalesce(p_start_date::text,''),
               'UTF8'
             ),
             'sha256'
           ),
           'hex'
         )
$$;

-- =============== upsert helpers ===============

-- Upsert club by API id
CREATE OR REPLACE FUNCTION upsert_club(
  p_api_club_id  INT,
  p_name         TEXT,
  p_abbr         CHAR(3) DEFAULT NULL,
  p_country_code CHAR(3) DEFAULT NULL,
  p_logo_url     TEXT     DEFAULT NULL
) RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
  INSERT INTO club (api_club_id, club_name, club_abbr, country_code, logo_url)
  VALUES (p_api_club_id, p_name, p_abbr, p_country_code, p_logo_url)
  ON CONFLICT (api_club_id) DO UPDATE
    SET club_name    = EXCLUDED.club_name,
        club_abbr    = COALESCE(EXCLUDED.club_abbr, club.club_abbr),
        country_code = COALESCE(EXCLUDED.country_code, club.country_code),
        logo_url     = COALESCE(EXCLUDED.logo_url, club.logo_url);
END$$;

-- Upsert player by API id
CREATE OR REPLACE FUNCTION upsert_player(
  p_player_api_id   INT,
  p_player_name     TEXT,
  p_dob             DATE     DEFAULT NULL,
  p_height_cm       SMALLINT DEFAULT NULL,
  p_nat_code        CHAR(3)  DEFAULT NULL,
  p_image_url       TEXT     DEFAULT NULL
) RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
  INSERT INTO player (player_api_id, player_name, date_of_birth, height_cm, nationality_code, image_url)
  VALUES (p_player_api_id, p_player_name, p_dob, p_height_cm, p_nat_code, p_image_url)
  ON CONFLICT (player_api_id) DO UPDATE
    SET player_name      = EXCLUDED.player_name,
        date_of_birth    = COALESCE(EXCLUDED.date_of_birth,    player.date_of_birth),
        height_cm        = COALESCE(EXCLUDED.height_cm,        player.height_cm),
        nationality_code = COALESCE(EXCLUDED.nationality_code, player.nationality_code),
        image_url        = COALESCE(EXCLUDED.image_url,        player.image_url);
END$$;

-- Ensure squad roster tuple exists / is updated
CREATE OR REPLACE FUNCTION ensure_squad_roster(
  p_season_id     TEXT,
  p_api_club_id   INT,
  p_player_api_id INT,
  p_position_code TEXT     DEFAULT NULL,
  p_shirt_number  INT      DEFAULT NULL,
  p_on_loan       BOOLEAN  DEFAULT NULL,
  p_loan_from     INT      DEFAULT NULL,
  p_join_date     DATE     DEFAULT NULL,
  p_leave_date    DATE     DEFAULT NULL
) RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
  INSERT INTO squad_roster (
    season_id, api_club_id, player_api_id, position_code, shirt_number,
    on_loan, loan_from_club, join_date, leave_date
  )
  VALUES (
    p_season_id, p_api_club_id, p_player_api_id, p_position_code, p_shirt_number,
    p_on_loan, p_loan_from, p_join_date, p_leave_date
  )
  ON CONFLICT (season_id, api_club_id, player_api_id) DO UPDATE
    SET position_code  = EXCLUDED.position_code,
        shirt_number   = EXCLUDED.shirt_number,
        on_loan        = EXCLUDED.on_loan,
        loan_from_club = EXCLUDED.loan_from_club,
        join_date      = EXCLUDED.join_date,
        leave_date     = EXCLUDED.leave_date,
        updated        = now();
END$$;

-- =============== convenience view ===============

CREATE OR REPLACE VIEW v_player_availability AS
SELECT
  p.player_api_id,
  p.player_name,
  p.image_url,
  p.nationality_code,
  c.api_club_id,
  c.club_name,
  c.club_abbr,
  c.logo_url,
  cur.status,
  cur.availability_type,
  cur.reason_subtype,
  cur.expected_return_date,
  cur.updated_at,
  cur.source_event_id
FROM player p
LEFT JOIN squad_roster r
  ON r.player_api_id = p.player_api_id
LEFT JOIN club c
  ON c.api_club_id = r.api_club_id
LEFT JOIN player_availability_current cur
  ON cur.player_api_id = p.player_api_id;
