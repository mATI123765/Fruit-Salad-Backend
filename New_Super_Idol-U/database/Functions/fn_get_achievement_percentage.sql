/* Function 2: This function returns the achievement completion percentage */
USE new_super_idol_u;

DELIMITER //

CREATE FUNCTION fn_get_achievement_percentage(
    p_user_id INT,
    p_game_id INT
)
RETURNS DECIMAL(5,2) -- Percentage with 2 decimals
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total_achievements INT DEFAULT 0;
    DECLARE v_unlocked_achievements INT DEFAULT 0;

    -- Count total achievements for this game
    SELECT COUNT(*) INTO v_total_achievements
    FROM achievement
    WHERE game_id = p_game_id;

    -- Count unlocked achievements for this user
    SELECT COUNT(*) INTO v_unlocked_achievements
    FROM user_achievement ua
    INNER JOIN achievement a ON ua.achievement_id = a.achievement_id
    WHERE ua.user_id = p_user_id
        AND a.game_id = p_game_id;
    
    -- Avoid division by zero
    IF v_total_achievements = 0 THEN
        RETURN 0.00;
    END IF;

    -- Return percentage
    RETURN ROUND((v_unlocked_achievement / v_total_achievements) * 100, 2);
END //

DELIMITER ;