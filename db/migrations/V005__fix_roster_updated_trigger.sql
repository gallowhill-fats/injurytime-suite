SET search_path TO public;

-- A dedicated function for the roster table's 'updated' column
CREATE OR REPLACE FUNCTION touch_updated()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated := now();
  RETURN NEW;
END$$;

-- Drop the broken trigger and re-create it with the correct function
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_roster_touch_updated') THEN
    EXECUTE 'DROP TRIGGER trg_roster_touch_updated ON squad_roster';
  END IF;

  CREATE TRIGGER trg_roster_touch_updated
  BEFORE UPDATE ON squad_roster
  FOR EACH ROW
  EXECUTE FUNCTION touch_updated();
END$$;

