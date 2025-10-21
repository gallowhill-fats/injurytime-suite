-- Raw JSON for each fixture's events (overwrite on refresh)
CREATE TABLE IF NOT EXISTS fixture_events_raw (
  fixture_id    BIGINT PRIMARY KEY REFERENCES fixture(fixture_id) ON DELETE CASCADE,
  fetched_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  payload       JSONB NOT NULL
);

-- One row per event
CREATE TABLE IF NOT EXISTS fixture_event (
  event_id        BIGSERIAL PRIMARY KEY,
  fixture_id      BIGINT NOT NULL REFERENCES fixture(fixture_id) ON DELETE CASCADE,
  seq_no          INT    NOT NULL,              -- position in response array (1..N)
  minute          INT,
  minute_extra    INT,
  team_api_id     INT,                          -- API team id
  player_api_id   INT,
  player_name     TEXT,
  assist_api_id   INT,
  assist_name     TEXT,
  ev_type         TEXT NOT NULL,                -- goal/card/subst/penalty/var/...
  ev_detail       TEXT,                         -- "Normal Goal", "Yellow Card", "Substitution 1", ...
  comments        TEXT,
  CONSTRAINT uq_fixture_event UNIQUE (fixture_id, seq_no)
);

CREATE INDEX IF NOT EXISTS idx_fixture_event_fixture ON fixture_event(fixture_id);
CREATE INDEX IF NOT EXISTS idx_fixture_event_player  ON fixture_event(player_api_id);
CREATE INDEX IF NOT EXISTS idx_fixture_event_team    ON fixture_event(team_api_id);
CREATE INDEX IF NOT EXISTS idx_fixture_event_type    ON fixture_event(ev_type);

