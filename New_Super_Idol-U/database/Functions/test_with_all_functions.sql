/* To test all functions I was make, you'll try with this comprehensive query: */
USE new_super_idol_u;

SELECT
    u.username,
    v.title AS GAME,
    fn_get_total_playtime(u.user_id, v.game_id) AS hours_played,
    fn_get_achievement_percentage(u.user_id, v.game_id) AS achievement_pct,
    fn_get_collection_percentage(u.user_id, v.game_id) AS collection_pct,
    fn_get_total_spent(u.user_id, v.game_id) AS money_spent
FROM user u
CROSS JOIN videogame v
WHERE u.user_id = 1
ORDER BY hours_played DESC;