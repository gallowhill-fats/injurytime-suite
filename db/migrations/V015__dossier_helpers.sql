-- V012__dossier_helpers.sql

DROP FUNCTION IF EXISTS public.is_finished(text);

-- Finished?
CREATE OR REPLACE FUNCTION public.is_finished(p_status text)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $$
  SELECT lower(coalesce(p_status,'')) IN ('ft','aet','pen')
$$;

-- ONE ROW / TEAM / MATCH already exists as v_team_match_row
-- Ensure it has: league_id, season, fixture_id, match_date_utc, status_short,
-- team_api_id, is_home, gf, ga, pts, res

-- 1) Team stats (season-to-date)
CREATE OR REPLACE FUNCTION team_stats(p_league int, p_season int, p_team int)
RETURNS TABLE(stat text, cnt int, pct numeric, per_game numeric) LANGUAGE sql STABLE AS $$
WITH rows AS (
  SELECT * FROM v_team_match_row r
  WHERE r.league_id=p_league AND r.season=p_season AND r.team_api_id=p_team AND is_finished(r.status_short)
),
g AS (SELECT COUNT(*) n FROM rows),
h AS (SELECT COUNT(*) n FROM rows WHERE is_home),
a AS (SELECT COUNT(*) n FROM rows WHERE NOT is_home)
SELECT * FROM (
  -- n, p, ppg
  SELECT 'n'::text, (SELECT n FROM g), NULL::numeric, NULL::numeric
  UNION ALL
  SELECT 'p', SUM(pts)::int, NULL, (SUM(pts)::numeric / NULLIF((SELECT n FROM g),0))
  FROM rows
  UNION ALL
  -- draws / wins / losses (overall + home/away)
  SELECT 'n_draw', SUM((res='D')::int), 100.0*SUM((res='D')::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL
  SELECT 'n_win', SUM((res='W')::int), 100.0*SUM((res='W')::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL
  SELECT 'n_loss', SUM((res='L')::int), 100.0*SUM((res='L')::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL
  SELECT 'ppgh', NULL, NULL, SUM(CASE WHEN is_home THEN pts ELSE 0 END)::numeric / NULLIF((SELECT n FROM h),0) FROM rows
  UNION ALL
  SELECT 'ppga', NULL, NULL, SUM(CASE WHEN NOT is_home THEN pts ELSE 0 END)::numeric / NULLIF((SELECT n FROM a),0) FROM rows
  UNION ALL
  -- gf/ga per game (overall/home/away)
  SELECT 'gfpg', SUM(gf)::int, NULL, SUM(gf)::numeric / NULLIF((SELECT n FROM g),0) FROM rows
  UNION ALL
  SELECT 'gfpgh', SUM(CASE WHEN is_home THEN gf ELSE 0 END)::int, NULL,
         SUM(CASE WHEN is_home THEN gf ELSE 0 END)::numeric / NULLIF((SELECT n FROM h),0) FROM rows
  UNION ALL
  SELECT 'gfpga', SUM(CASE WHEN NOT is_home THEN gf ELSE 0 END)::int, NULL,
         SUM(CASE WHEN NOT is_home THEN gf ELSE 0 END)::numeric / NULLIF((SELECT n FROM a),0) FROM rows
  UNION ALL
  SELECT 'gapg', SUM(ga)::int, NULL, SUM(ga)::numeric / NULLIF((SELECT n FROM g),0) FROM rows
  UNION ALL
  SELECT 'gapgh', SUM(CASE WHEN is_home THEN ga ELSE 0 END)::int, NULL,
         SUM(CASE WHEN is_home THEN ga ELSE 0 END)::numeric / NULLIF((SELECT n FROM h),0) FROM rows
  UNION ALL
  SELECT 'gapga', SUM(CASE WHEN NOT is_home THEN ga ELSE 0 END)::int, NULL,
         SUM(CASE WHEN NOT is_home THEN ga ELSE 0 END)::numeric / NULLIF((SELECT n FROM a),0) FROM rows
  UNION ALL
  -- clean sheets
  SELECT 'ncs',  SUM((ga=0)::int), 100.0*SUM((ga=0)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL
  SELECT 'ncsh', SUM((is_home AND ga=0)::int), 100.0*SUM((is_home AND ga=0)::int)/NULLIF((SELECT n FROM h),0), NULL FROM rows
  UNION ALL
  SELECT 'ncsa', SUM((NOT is_home AND ga=0)::int), 100.0*SUM((NOT is_home AND ga=0)::int)/NULLIF((SELECT n FROM a),0), NULL FROM rows
  UNION ALL
  -- O/U goal lines
  SELECT 'no15', SUM(((gf+ga)>1)::int), 100.0*SUM(((gf+ga)>1)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL SELECT 'nu15', SUM(((gf+ga)<=1)::int), 100.0*SUM(((gf+ga)<=1)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL SELECT 'no25', SUM(((gf+ga)>2)::int), 100.0*SUM(((gf+ga)>2)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL SELECT 'nu25', SUM(((gf+ga)<=2)::int), 100.0*SUM(((gf+ga)<=2)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL SELECT 'no35', SUM(((gf+ga)>3)::int), 100.0*SUM(((gf+ga)>3)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL SELECT 'nu35', SUM(((gf+ga)<=3)::int), 100.0*SUM(((gf+ga)<=3)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL SELECT 'no45', SUM(((gf+ga)>4)::int), 100.0*SUM(((gf+ga)>4)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL SELECT 'nu45', SUM(((gf+ga)<=4)::int), 100.0*SUM(((gf+ga)<=4)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL
  -- BTTS
  SELECT 'nbts',  SUM(((gf>0) AND (ga>0))::int), 100.0*SUM(((gf>0) AND (ga>0))::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
  UNION ALL
  SELECT 'nbtsh', SUM((is_home AND gf>0 AND ga>0)::int), 100.0*SUM((is_home AND gf>0 AND ga>0)::int)/NULLIF((SELECT n FROM h),0), NULL FROM rows
  UNION ALL
  SELECT 'nbtsa', SUM((NOT is_home AND gf>0 AND ga>0)::int), 100.0*SUM((NOT is_home AND gf>0 AND ga>0)::int)/NULLIF((SELECT n FROM a),0), NULL FROM rows
) s(stat, cnt, pct, per_game)
ORDER BY stat;
$$;

-- 2) Last N points (most recent first)
CREATE OR REPLACE FUNCTION team_last_points(p_league int, p_season int, p_team int, p_n int)
RETURNS TABLE(match_date_utc timestamp, week int, pts int) LANGUAGE sql STABLE AS $$
SELECT r.match_date_utc, NULLIF(substring(coalesce(f.round_name,'') FROM '([0-9]+)$'), '')::int AS week,
       r.pts
FROM v_team_match_row r
JOIN fixture f ON f.fixture_id = r.fixture_id
WHERE r.league_id=p_league AND r.season=p_season AND r.team_api_id=p_team AND is_finished(r.status_short)
ORDER BY r.match_date_utc DESC
LIMIT p_n;
$$;

-- 3) Score histograms
CREATE OR REPLACE FUNCTION score_hist_team(p_league int, p_season int, p_team int, p_home_only boolean, p_away_only boolean)
RETURNS TABLE(gf int, ga int, n bigint) LANGUAGE sql STABLE AS $$
SELECT gf, ga, COUNT(*)::bigint
FROM v_team_match_row r
WHERE r.league_id=p_league AND r.season=p_season AND r.team_api_id=p_team AND is_finished(r.status_short)
  AND (NOT p_home_only OR r.is_home)
  AND (NOT p_away_only OR NOT r.is_home)
GROUP BY gf, ga
ORDER BY gf, ga;
$$;

CREATE OR REPLACE FUNCTION score_hist_league(p_league int, p_season int)
RETURNS TABLE(gf int, ga int, n bigint) LANGUAGE sql STABLE AS $$
SELECT ft.ft_home_goals AS gf, ft.ft_away_goals AS ga, COUNT(*)::bigint
FROM fixture f
JOIN fixture_teams ft ON ft.fixture_id=f.fixture_id
WHERE f.league_id=p_league AND f.season=p_season AND is_finished(f.status_short)
GROUP BY ft.ft_home_goals, ft.ft_away_goals
ORDER BY gf, ga;
$$;

-- 4) Last 5 summaries (goals + red cards)
CREATE OR REPLACE FUNCTION team_last5_summary(p_league int, p_season int, p_team int)
RETURNS jsonb LANGUAGE sql STABLE AS $$
WITH last5 AS (
  SELECT r.fixture_id, r.is_home,
         CASE WHEN r.is_home THEN r.gf||'-'||r.ga ELSE r.ga||'-'||r.gf END AS score_txt,
         r.match_date_utc,
         (SELECT CASE WHEN r.is_home THEN ft.away_team ELSE ft.home_team END
          FROM fixture_teams ft WHERE ft.fixture_id=r.fixture_id) AS opponent
  FROM v_team_match_row r
  WHERE r.league_id=p_league AND r.season=p_season AND r.team_api_id=p_team AND is_finished(r.status_short)
  ORDER BY r.match_date_utc DESC
  LIMIT 5
)
SELECT jsonb_agg(
  jsonb_build_object(
    'fixture_id', f.fixture_id,
    'when_utc',   f.match_date_utc,
    'home',       f.is_home,
    'opponent',   f.opponent,
    'score',      f.score_txt,
    'goals',      (SELECT jsonb_agg(jsonb_build_object(
                     'minute', fe.minute, 'extra', fe.minute_extra, 'player', fe.player_name,
                     'detail', fe.ev_detail))
                   FROM fixture_event fe
                   WHERE fe.fixture_id=f.fixture_id AND fe.ev_type='goal'),
    'reds',       (SELECT jsonb_agg(jsonb_build_object(
                     'minute', fe.minute, 'extra', fe.minute_extra, 'player', fe.player_name,
                     'detail', fe.ev_detail))
                   FROM fixture_event fe
                   WHERE fe.fixture_id=f.fixture_id AND fe.ev_type='card' AND lower(fe.ev_detail) LIKE 'red%')
  ) ORDER BY f.match_date_utc DESC
)
FROM last5 f;
$$;

-- Top scorers for a team in a league/season
-- Uses player.player_name when available; otherwise falls back to the most
-- common non-null name seen on events for that player_api_id.
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
  WITH goals AS (
    SELECT ev.player_api_id, COUNT(*)::int AS goals
    FROM fixture_event ev
    JOIN fixture f ON f.fixture_id = ev.fixture_id
    WHERE f.league_id    = p_league_id
      AND f.season       = p_season
      AND ev.team_api_id = p_team
      AND ev.ev_type     = 'Goal'
      AND COALESCE(ev.ev_detail,'') <> 'Own Goal'
    GROUP BY ev.player_api_id
  ),
  ev_names AS (
    -- any stable non-null name from events for that player id
    SELECT ev.player_api_id, MAX(ev.player_name) AS ev_name
    FROM fixture_event ev
    WHERE ev.player_api_id IS NOT NULL
    GROUP BY ev.player_api_id
  )
  SELECT
    g.player_api_id,
    COALESCE(p.player_name, en.ev_name, 'Unknown') AS player_name,
    g.goals
  FROM goals g
  LEFT JOIN player p
    ON p.player_api_id IS NOT DISTINCT FROM g.player_api_id
  LEFT JOIN ev_names en
    ON en.player_api_id IS NOT DISTINCT FROM g.player_api_id
  ORDER BY g.goals DESC, player_name
  LIMIT p_limit;
$$;




-- 6) Current unavailability (customize as your availability model evolves)
CREATE OR REPLACE FUNCTION current_unavailability(p_team int)
RETURNS TABLE(player_api_id int, player_name text, availability_type text, status text, notes text) LANGUAGE sql STABLE AS $$
SELECT sr.player_api_id,
       p.player_name AS player_name,
       ae.availability_type,
       ae.status,
       NULL::text AS notes
FROM squad_roster sr
JOIN availability_events ae ON ae.player_api_id = sr.player_api_id
LEFT JOIN player p ON p.player_api_id = sr.player_api_id
WHERE sr.api_club_id = p_team
  AND ae.id IN (
    SELECT MAX(id) FROM availability_events ae2
    WHERE ae2.player_api_id=sr.player_api_id
  ) -- last status per player
  AND lower(coalesce(ae.status,'')) IN ('out','doubtful','unknown'); -- tweak filter if needed
$$;

