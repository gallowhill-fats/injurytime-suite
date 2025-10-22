-- V011__week_helpers_and_table.sql
-- Helpers + form + standings using round_name's trailing number as "week"

-- 1) Parse trailing week number from fixture.round_name (NULL if none)
CREATE OR REPLACE FUNCTION fixture_week_no(p_round_name text)
RETURNS int
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
  SELECT NULLIF(substring(coalesce(p_round_name, '') FROM '([0-9]+)$'), '')::int
$$;

-- 2) Normalize finished statuses
CREATE OR REPLACE FUNCTION is_finished(p_status text)
RETURNS boolean LANGUAGE sql IMMUTABLE AS $$
  SELECT lower(coalesce(p_status,'')) IN ('ft','aet','pen')
$$;

-- 3) Base views (safe to re-create)
CREATE OR REPLACE VIEW v_team_match_result AS
SELECT
  f.league_id,
  f.season,
  f.fixture_id,
  f.status_short,
  f.match_date_utc,
  ft.home_team_id  AS team_api_id_home,
  ft.away_team_id  AS team_api_id_away,
  ft.ft_home_goals,
  ft.ft_away_goals,
  CASE
    WHEN ft.ft_home_goals > ft.ft_away_goals THEN 'H'
    WHEN ft.ft_home_goals < ft.ft_away_goals THEN 'A'
    ELSE 'D'
  END AS winner
FROM fixture f
JOIN fixture_teams ft ON ft.fixture_id = f.fixture_id;

CREATE OR REPLACE VIEW v_team_match_row AS
SELECT
  league_id, season, fixture_id, match_date_utc,
  status_short,
  team_api_id_home AS team_api_id,
  TRUE  AS is_home,
  ft_home_goals AS gf,
  ft_away_goals AS ga,
  CASE
    WHEN ft_home_goals > ft_away_goals THEN 3
    WHEN ft_home_goals = ft_away_goals THEN 1
    ELSE 0
  END AS pts,
  CASE WHEN ft_home_goals > ft_away_goals THEN 'W'
       WHEN ft_home_goals = ft_away_goals THEN 'D'
       ELSE 'L' END AS res
FROM v_team_match_result
UNION ALL
SELECT
  league_id, season, fixture_id, match_date_utc,
  status_short,
  team_api_id_away AS team_api_id,
  FALSE AS is_home,
  ft_away_goals AS gf,
  ft_home_goals AS ga,
  CASE
    WHEN ft_away_goals > ft_home_goals THEN 3
    WHEN ft_away_goals = ft_home_goals THEN 1
    ELSE 0
  END AS pts,
  CASE WHEN ft_away_goals > ft_home_goals THEN 'W'
       WHEN ft_away_goals = ft_home_goals THEN 'D'
       ELSE 'L' END AS res
FROM v_team_match_result;

-- 4) Last N results string like 'WWDLW' up to optional max week
CREATE OR REPLACE FUNCTION team_last_form(
  p_league_id  int,
  p_season     int,
  p_team       int,
  p_limit      int,
  p_max_week   int DEFAULT NULL
) RETURNS text
LANGUAGE sql STABLE AS $$
  SELECT string_agg(r.res, '' ORDER BY r.match_date_utc DESC)
  FROM (
    SELECT r.res, r.match_date_utc
    FROM v_team_match_row r
    JOIN fixture f ON f.fixture_id = r.fixture_id
    WHERE r.league_id   = p_league_id
      AND r.season      = p_season
      AND r.team_api_id = p_team
      AND is_finished(r.status_short)
      AND (p_max_week IS NULL OR fixture_week_no(f.round_name) <= p_max_week)
    ORDER BY r.match_date_utc DESC
    LIMIT p_limit
  ) r;
$$;

-- 5) Standings as-of optional max week (aggregates home/away splits)
CREATE OR REPLACE FUNCTION league_table_as_of(
  p_league_id int,
  p_season    int,
  p_max_week  int DEFAULT NULL
)
RETURNS TABLE(
  team_api_id int,
  played int,
  won int,
  drawn int,
  lost int,
  gf_home int,
  ga_home int,
  gf_away int,
  ga_away int,
  gf int,
  ga int,
  gd int,
  pts int
) LANGUAGE sql STABLE AS $$
  WITH rows AS (
    SELECT r.*
    FROM v_team_match_row r
    JOIN fixture f ON f.fixture_id = r.fixture_id
    WHERE r.league_id = p_league_id
      AND r.season    = p_season
      AND is_finished(r.status_short)
      AND (p_max_week IS NULL OR fixture_week_no(f.round_name) <= p_max_week)
  )
  SELECT
    team_api_id,
    COUNT(*)                                     AS played,
    SUM(CASE WHEN res='W' THEN 1 ELSE 0 END)     AS won,
    SUM(CASE WHEN res='D' THEN 1 ELSE 0 END)     AS drawn,
    SUM(CASE WHEN res='L' THEN 1 ELSE 0 END)     AS lost,
    SUM(CASE WHEN is_home THEN gf ELSE 0 END)      AS gf_home,
    SUM(CASE WHEN is_home THEN ga ELSE 0 END)      AS ga_home,
    SUM(CASE WHEN NOT is_home THEN gf ELSE 0 END)  AS gf_away,
    SUM(CASE WHEN NOT is_home THEN ga ELSE 0 END)  AS ga_away,
    SUM(gf)                                      AS gf,
    SUM(ga)                                      AS ga,
    SUM(gf) - SUM(ga)                            AS gd,
    SUM(pts)                                     AS pts
  FROM rows
  GROUP BY team_api_id
$$;

-- 6) Indexes for speed
CREATE INDEX IF NOT EXISTS idx_fixture_league_season ON fixture(league_id, season);
CREATE INDEX IF NOT EXISTS idx_fixture_week_expr
  ON fixture(league_id, season, (fixture_week_no(round_name)));
