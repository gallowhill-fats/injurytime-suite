-- Add imported_at and backfill with existing match_date_utc (or now)
ALTER TABLE fixture
  ADD COLUMN IF NOT EXISTS imported_at TIMESTAMPTZ;

-- Backfill once (pick ONE of these strategies):
-- A) Use match date
UPDATE fixture SET imported_at = COALESCE(imported_at, match_date_utc);
-- B) Or, use current time for rows missing it
-- UPDATE fixture SET imported_at = COALESCE(imported_at, now());

-- Optional: keep it fresh on every write
CREATE OR REPLACE FUNCTION trg_touch_imported_at()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  NEW.imported_at := COALESCE(NEW.imported_at, now());
  RETURN NEW;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_fixture_touch_imported_at') THEN
    CREATE TRIGGER trg_fixture_touch_imported_at
      BEFORE INSERT OR UPDATE ON fixture
      FOR EACH ROW EXECUTE FUNCTION trg_touch_imported_at();
  END IF;
END $$;

