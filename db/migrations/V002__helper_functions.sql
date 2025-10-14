-- Optional: small helper to upsert player current status
CREATE OR REPLACE FUNCTION set_player_current_availability(
p_player_id BIGINT,
p_status TEXT,
p_type TEXT,
p_subtype TEXT,
p_event_id BIGINT,
p_expected_return DATE
) RETURNS VOID AS $$
BEGIN
INSERT INTO player_availability_current (player_id, status, availability_type, reason_subtype, updated_at, source_event_id, expected_return_date)
VALUES (p_player_id, p_status, p_type, p_subtype, now(), p_event_id, p_expected_return)
ON CONFLICT (player_id)
DO UPDATE SET
status = EXCLUDED.status,
availability_type = EXCLUDED.availability_type,
reason_subtype = EXCLUDED.reason_subtype,
updated_at = now(),
source_event_id = EXCLUDED.source_event_id,
expected_return_date = EXCLUDED.expected_return_date;
END;
$$ LANGUAGE plpgsql;
