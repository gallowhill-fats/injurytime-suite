-- V001__availability_core.sql
SET search_path TO public;

-- Raw capture
CREATE TABLE IF NOT EXISTS availability_events_raw (
  id             BIGSERIAL PRIMARY KEY,
  source_system  TEXT NOT NULL,        -- gmail, rss, web, api
  source_msg_id  TEXT,                 -- gmail msg id, RSS guid, URL hash
  fetched_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  source_uri     TEXT,                 -- email display url, rss link, page url
  subject        TEXT,
  raw_html       TEXT,
  raw_text       TEXT,
  UNIQUE (source_system, source_msg_id)
);

-- Normalized events (FK by API IDs to master)
CREATE TABLE IF NOT EXISTS availability_events (
  id                    BIGSERIAL PRIMARY KEY,
  raw_id                BIGINT  REFERENCES availability_events_raw(id) ON DELETE CASCADE,
  player_api_id         INTEGER NULL REFERENCES player(player_api_id),
  api_club_id           INTEGER NULL REFERENCES club(api_club_id),

  availability_type     TEXT NOT NULL, -- injury, suspension, illness, rest, etc.
  reason_subtype        TEXT NULL,     -- hamstring, ACL, red_card, yellow_accumulation
  status                TEXT NOT NULL, -- out, doubtful, probable, available, unknown
  start_date            DATE NULL,
  expected_return_date  DATE NULL,
  expected_duration_days INT NULL,
  confidence            INT  NOT NULL CHECK (confidence BETWEEN 0 AND 100),
  headline              TEXT,
  snippet               TEXT,
  canonical_article_url TEXT,
  first_seen            TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_av_events_player_api ON availability_events(player_api_id);
CREATE INDEX IF NOT EXISTS idx_av_events_club_api   ON availability_events(api_club_id);
CREATE INDEX IF NOT EXISTS idx_av_events_type       ON availability_events(availability_type);
CREATE INDEX IF NOT EXISTS idx_av_events_status     ON availability_events(status);

-- Claims
CREATE TABLE IF NOT EXISTS availability_claims (
  id                BIGSERIAL PRIMARY KEY,
  event_id          BIGINT REFERENCES availability_events(id) ON DELETE CASCADE,
  source_system     TEXT NOT NULL,
  source_uri        TEXT,
  claim_hash        TEXT NOT NULL,
  claim_confidence  INT NOT NULL CHECK (claim_confidence BETWEEN 0 AND 100),
  seen_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_av_claims_hash ON availability_claims(claim_hash);

-- Current availability (by API ID)
CREATE TABLE IF NOT EXISTS player_availability_current (
  player_api_id        INTEGER PRIMARY KEY REFERENCES player(player_api_id),
  status               TEXT NOT NULL, -- out/doubtful/probable/available/unknown
  availability_type    TEXT NULL,
  reason_subtype       TEXT NULL,
  updated_at           TIMESTAMPTZ NOT NULL,
  source_event_id      BIGINT REFERENCES availability_events(id),
  expected_return_date DATE NULL
);

