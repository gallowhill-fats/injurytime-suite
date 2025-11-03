-- Top scorers for a team (own goals excluded)
CREATE OR REPLACE FUNCTION team_top_scorers(
  p_league_id int,
  p_season    int,
  p_team      int,
  p_limit     int DEFAULT 5
)
RETURNS TABLE(
  player_api_id int,
  player_name   text,
  goals         int
)
LANGUAGE sql STABLE AS $$
  SELECT
    ev.player_api_id,
    COALESCE(p.player_name, ev.player_name) AS player_name,
    COUNT(*)::int                            AS goals
  FROM fixture_event ev
  JOIN fixture f ON f.fixture_id = ev.fixture_id
  LEFT JOIN player p ON p.api_player_id = ev.player_api_id
  WHERE f.league_id   = p_league_id
    AND f.season      = p_season
    AND ev.team_api_id = p_team
    AND ev.ev_type     = 'Goal'
    AND COALESCE(ev.ev_detail,'') <> 'Own Goal'  -- don’t credit OGs
  GROUP BY ev.player_api_id, COALESCE(p.player_name, ev.player_name)
  ORDER BY goals DESC, player_name
  LIMIT p_limit;
$$;