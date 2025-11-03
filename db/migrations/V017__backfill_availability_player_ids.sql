-- V017__backfill_availability_player_ids.sql
-- Backfill availability_events.player_api_id by fuzzy matching
-- against player_alias.alias and player.player_name in headline/snippet.

-- Pass 1: match via player_alias (same club), prefer headline matches and longer aliases
WITH cand AS (
  SELECT
    ae.id                          AS ae_id,
    sr.player_api_id               AS pid,
    CASE
      WHEN ae.headline ILIKE '%' || pa.alias || '%' THEN 2
      WHEN ae.snippet  ILIKE '%' || pa.alias || '%' THEN 1
      ELSE 0
    END                            AS score,
    LENGTH(pa.alias)               AS alen
  FROM availability_events ae
  JOIN squad_roster sr
    ON sr.api_club_id = ae.api_club_id
  JOIN player_alias pa
    ON pa.player_api_id = sr.player_api_id
  WHERE ae.player_api_id IS NULL
    AND pa.alias IS NOT NULL
    AND LENGTH(pa.alias) >= 3
    AND (
      ae.headline ILIKE '%' || pa.alias || '%' OR
      ae.snippet  ILIKE '%' || pa.alias || '%'
    )
),
pick AS (
  SELECT DISTINCT ON (ae_id)
    ae_id, pid
  FROM cand
  ORDER BY ae_id, score DESC, alen DESC
)
UPDATE availability_events ae
SET player_api_id = p.pid
FROM pick p
WHERE ae.id = p.ae_id;

-- Pass 2: where still NULL, try matching the canonical player name
WITH cand2 AS (
  SELECT
    ae.id                AS ae_id,
    sr.player_api_id     AS pid,
    CASE
      WHEN ae.headline ILIKE '%' || p.player_name || '%' THEN 2
      WHEN ae.snippet  ILIKE '%' || p.player_name || '%' THEN 1
      ELSE 0
    END                  AS score,
    LENGTH(p.player_name) AS alen
  FROM availability_events ae
  JOIN squad_roster sr
    ON sr.api_club_id = ae.api_club_id
  JOIN player p
    ON p.player_api_id = sr.player_api_id
  WHERE ae.player_api_id IS NULL
    AND p.player_name IS NOT NULL
    AND LENGTH(p.player_name) >= 3
    AND (
      ae.headline ILIKE '%' || p.player_name || '%' OR
      ae.snippet  ILIKE '%' || p.player_name || '%'
    )
),
pick2 AS (
  SELECT DISTINCT ON (ae_id)
    ae_id, pid
  FROM cand2
  ORDER BY ae_id, score DESC, alen DESC
)
UPDATE availability_events ae
SET player_api_id = p.pid
FROM pick2 p
WHERE ae.id = p.ae_id;

-- (Optional) Report how many remain unmatched
-- SELECT COUNT(*) AS still_null FROM availability_events WHERE player_api_id IS NULL;
