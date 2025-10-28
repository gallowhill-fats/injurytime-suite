-- V011__club_sources.sql

-- One row per source URL per club
CREATE TABLE IF NOT EXISTS club_source (
  id             BIGSERIAL PRIMARY KEY,
  api_club_id    INT NOT NULL
                  REFERENCES club(api_club_id) ON DELETE CASCADE,

  source_type    TEXT NOT NULL CHECK (source_type IN (
                   'official',          -- official club site or specific page
                   'unofficial',        -- fan/blog
                   'team_news',         -- a team news section/page
                   'local_news'         -- local newspaper/sports page
                 )),

  label          TEXT NOT NULL,         -- e.g. "Club site", "Surrey Live"
  url            TEXT NOT NULL,
  -- normalized (lowercased) URL for uniqueness and ON CONFLICT target
  url_norm       TEXT GENERATED ALWAYS AS (lower(url)) STORED,

  rss_url        TEXT NULL,             -- if the source exposes RSS/Atom
  is_official    BOOLEAN NOT NULL DEFAULT FALSE,
  priority       INT NOT NULL DEFAULT 100,   -- lower = higher priority
  active         BOOLEAN NOT NULL DEFAULT TRUE,
  last_checked   TIMESTAMPTZ,
  notes          TEXT,

  -- Case-insensitive uniqueness enforced via generated column
  CONSTRAINT uq_club_source UNIQUE (api_club_id, url_norm)
);

CREATE INDEX IF NOT EXISTS idx_club_source_club    ON club_source(api_club_id);
CREATE INDEX IF NOT EXISTS idx_club_source_active  ON club_source(active);
CREATE INDEX IF NOT EXISTS idx_club_source_pri     ON club_source(priority);

-- Convenience UPSERT helper
CREATE OR REPLACE FUNCTION upsert_club_source(
  p_api_club_id INT,
  p_source_type TEXT,
  p_label       TEXT,
  p_url         TEXT,
  p_rss_url     TEXT DEFAULT NULL,
  p_is_official BOOLEAN DEFAULT FALSE,
  p_priority    INT DEFAULT 100,
  p_active      BOOLEAN DEFAULT TRUE,
  p_notes       TEXT DEFAULT NULL
) RETURNS VOID
LANGUAGE plpgsql AS $$
BEGIN
  INSERT INTO club_source (api_club_id, source_type, label, url, rss_url,
                           is_official, priority, active, notes)
  VALUES (p_api_club_id, p_source_type, p_label, p_url, p_rss_url,
          COALESCE(p_is_official, FALSE),
          COALESCE(p_priority, 100),
          COALESCE(p_active, TRUE),
          p_notes)
  ON CONFLICT (api_club_id, url_norm) DO UPDATE
    SET source_type = EXCLUDED.source_type,
        label       = EXCLUDED.label,
        url         = EXCLUDED.url,        -- will recompute url_norm
        rss_url     = EXCLUDED.rss_url,
        is_official = EXCLUDED.is_official,
        priority    = EXCLUDED.priority,
        active      = EXCLUDED.active,
        notes       = EXCLUDED.notes;
END $$;

