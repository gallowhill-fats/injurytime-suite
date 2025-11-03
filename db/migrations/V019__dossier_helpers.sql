-- V019__dossier_helpers.sql
-- Recreate helpers with updated shapes/names.

-- 1) Keep the original parameter name for is_finished (avoid “cannot change name”)
CREATE OR REPLACE FUNCTION is_finished(p_status text)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $$
  SELECT lower(coalesce(p_status,'')) IN ('ft','aet','pen');
$$;

-- 2) team_stats: drop first because the TABLE return type changed
DROP FUNCTION IF EXISTS team_stats(integer, integer, integer);

-- 2a) Recreate team_stats with the new OUT columns (stat, cnt, pct, per_game)
CREATE FUNCTION team_stats(
  p_league_id int,
  p_season    int,
  p_team      int
)
RETURNS TABLE(
  stat      text,
  cnt       int,
  pct       numeric,
  per_game  numeric
)
LANGUAGE sql
STABLE
AS $$
  WITH rows AS (
    SELECT
      r.is_home,
      r.gf,
      r.ga,
      r.pts,
      r.res
    FROM v_team_match_row r
    WHERE r.league_id   = p_league_id
      AND r.season      = p_season
      AND r.team_api_id = p_team
      AND is_finished(r.status_short)
  ),
  g AS (SELECT COUNT(*)::int AS n FROM rows),
  h AS (SELECT COUNT(*)::int AS n FROM rows WHERE is_home),
  a AS (SELECT COUNT(*)::int AS n FROM rows WHERE NOT is_home)

  SELECT * FROM (
    -- n (games)
    SELECT 'n'::text                       AS stat,
           (SELECT n FROM g)               AS cnt,
           NULL::numeric                   AS pct,
           NULL::numeric                   AS per_game
    UNION ALL
    -- total points & points per game
    SELECT 'p', SUM(pts)::int, NULL::numeric,
           (SUM(pts)::numeric / NULLIF((SELECT n FROM g),0))
    FROM rows
    UNION ALL
    -- draws / wins / losses (overall)
    SELECT 'n_draw', SUM((res='D')::int),
           100.0*SUM((res='D')::int)/NULLIF((SELECT n FROM g),0), NULL
    FROM rows
    UNION ALL
    SELECT 'n_win',  SUM((res='W')::int),
           100.0*SUM((res='W')::int)/NULLIF((SELECT n FROM g),0), NULL
    FROM rows
    UNION ALL
    SELECT 'n_loss', SUM((res='L')::int),
           100.0*SUM((res='L')::int)/NULLIF((SELECT n FROM g),0), NULL
    FROM rows
    UNION ALL
    -- home/away points per game
    SELECT 'ppgh', NULL::int, NULL::numeric,
           SUM(CASE WHEN is_home THEN pts ELSE 0 END)::numeric / NULLIF((SELECT n FROM h),0)
    FROM rows
    UNION ALL
    SELECT 'ppga', NULL::int, NULL::numeric,
           SUM(CASE WHEN NOT is_home THEN pts ELSE 0 END)::numeric / NULLIF((SELECT n FROM a),0)
    FROM rows
    UNION ALL
    -- gf/ga per game (overall/home/away)
    SELECT 'gfpg', SUM(gf)::int, NULL::numeric,
           SUM(gf)::numeric / NULLIF((SELECT n FROM g),0)
    FROM rows
    UNION ALL
    SELECT 'gfpgh',
           SUM(CASE WHEN is_home THEN gf ELSE 0 END)::int, NULL::numeric,
           SUM(CASE WHEN is_home THEN gf ELSE 0 END)::numeric / NULLIF((SELECT n FROM h),0)
    FROM rows
    UNION ALL
    SELECT 'gfpga',
           SUM(CASE WHEN NOT is_home THEN gf ELSE 0 END)::int, NULL::numeric,
           SUM(CASE WHEN NOT is_home THEN gf ELSE 0 END)::numeric / NULLIF((SELECT n FROM a),0)
    FROM rows
    UNION ALL
    SELECT 'gapg', SUM(ga)::int, NULL::numeric,
           SUM(ga)::numeric / NULLIF((SELECT n FROM g),0)
    FROM rows
    UNION ALL
    SELECT 'gapgh',
           SUM(CASE WHEN is_home THEN ga ELSE 0 END)::int, NULL::numeric,
           SUM(CASE WHEN is_home THEN ga ELSE 0 END)::numeric / NULLIF((SELECT n FROM h),0)
    FROM rows
    UNION ALL
    SELECT 'gapga',
           SUM(CASE WHEN NOT is_home THEN ga ELSE 0 END)::int, NULL::numeric,
           SUM(CASE WHEN NOT is_home THEN ga ELSE 0 END)::numeric / NULLIF((SELECT n FROM a),0)
    FROM rows
    UNION ALL
    -- clean sheets (overall/home/away)
    SELECT 'ncs',  SUM((ga=0)::int),
           100.0*SUM((ga=0)::int)/NULLIF((SELECT n FROM g),0), NULL
    FROM rows
    UNION ALL
    SELECT 'ncsh', SUM((is_home AND ga=0)::int),
           100.0*SUM((is_home AND ga=0)::int)/NULLIF((SELECT n FROM h),0), NULL
    FROM rows
    UNION ALL
    SELECT 'ncsa', SUM((NOT is_home AND ga=0)::int),
           100.0*SUM((NOT is_home AND ga=0)::int)/NULLIF((SELECT n FROM a),0), NULL
    FROM rows
    UNION ALL
    -- O/U totals
    SELECT 'no15', SUM(((gf+ga)>1)::int),
           100.0*SUM(((gf+ga)>1)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
    UNION ALL
    SELECT 'nu15', SUM(((gf+ga)<=1)::int),
           100.0*SUM(((gf+ga)<=1)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
    UNION ALL
    SELECT 'no25', SUM(((gf+ga)>2)::int),
           100.0*SUM(((gf+ga)>2)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
    UNION ALL
    SELECT 'nu25', SUM(((gf+ga)<=2)::int),
           100.0*SUM(((gf+ga)<=2)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
    UNION ALL
    SELECT 'no35', SUM(((gf+ga)>3)::int),
           100.0*SUM(((gf+ga)>3)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
    UNION ALL
    SELECT 'nu35', SUM(((gf+ga)<=3)::int),
           100.0*SUM(((gf+ga)<=3)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
    UNION ALL
    SELECT 'no45', SUM(((gf+ga)>4)::int),
           100.0*SUM(((gf+ga)>4)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
    UNION ALL
    SELECT 'nu45', SUM(((gf+ga)<=4)::int),
           100.0*SUM(((gf+ga)<=4)::int)/NULLIF((SELECT n FROM g),0), NULL FROM rows
    UNION ALL
    -- BTTS (overall/home/away)
    SELECT 'nbts',  SUM(((gf>0) AND (ga>0))::int),
           100.0*SUM(((gf>0) AND (ga>0))::int)/NULLIF((SELECT n FROM g),0), NULL
    FROM rows
    UNION ALL
    SELECT 'nbtsh', SUM((is_home AND gf>0 AND ga>0)::int),
           100.0*SUM((is_home AND gf>0 AND ga>0)::int)/NULLIF((SELECT n FROM h),0), NULL
    FROM rows
    UNION ALL
    SELECT 'nbtsa', SUM((NOT is_home AND gf>0 AND ga>0)::int),
           100.0*SUM((NOT is_home AND gf>0 AND ga>0)::int)/NULLIF((SELECT n FROM a),0), NULL
    FROM rows
  ) s
  ORDER BY stat;
$$;
