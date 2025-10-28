-- V0xx__upsert_club_helper.sql
CREATE OR REPLACE FUNCTION upsert_club(
  p_api_club_id  INT,
  p_name         TEXT,
  p_abbr         CHAR(3) DEFAULT NULL,
  p_country_code CHAR(3) DEFAULT NULL,
  p_logo_url     TEXT     DEFAULT NULL
) RETURNS VOID
LANGUAGE plpgsql AS $$
BEGIN
  INSERT INTO club (api_club_id, club_name, club_abbr, country_code, logo_url)
  VALUES (p_api_club_id, p_name, p_abbr, p_country_code, p_logo_url)
  ON CONFLICT (api_club_id) DO UPDATE
    SET club_name = COALESCE(EXCLUDED.club_name, club.club_name),
        club_abbr = COALESCE(EXCLUDED.club_abbr, club.club_abbr),
        country_code = COALESCE(EXCLUDED.country_code, club.country_code),
        logo_url = COALESCE(EXCLUDED.logo_url, club.logo_url);
END$$;

