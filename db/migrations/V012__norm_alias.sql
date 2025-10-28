-- V012__norm_alias.sql
-- Normalizer + uniqueness for player aliases

-- Normalise aliases: trim, collapse whitespace, lowercase; return NULL if empty
CREATE OR REPLACE FUNCTION norm_alias(p text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
  SELECT NULLIF(
           lower( regexp_replace( trim( coalesce(p, '') ), '\s+', ' ', 'g') ),
           ''
         )
$$;

-- Ensure (player_api_id, alias) is unique.
-- If you already store aliases normalised (recommended), this is enough.
-- If you later decide to store mixed-case, switch to UNIQUE (player_api_id, lower(alias))
CREATE UNIQUE INDEX IF NOT EXISTS uq_player_alias
  ON player_alias(player_api_id, alias);

