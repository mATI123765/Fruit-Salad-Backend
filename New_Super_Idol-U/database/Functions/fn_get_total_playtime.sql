/* Function 1: This function returns the total playtime in hours */
USE new_super_idol_u;

DROP FUNCTION IF EXISTS fn_get_total_playtime;

DELIMITER $$

CREATE FUNCTION fn_get_total_playtime(
    p_user_id INT,
    p_game_id INT
)
RETURNS DECIMAL(10,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total_minutes INT DEFAULT 0;

    -- Sum all session durations
    SELECT COALESCE(SUM(duration_minutes), 0) INTO v_total_minutes
    FROM playtime
    WHERE user_id = p_user_id
        AND game_id = p_game_id
        AND end_time IS NOT NULL;

    -- Return hours (with 2 decimals)
    RETURN ROUND(v_total_minutes / 60, 2);
END$$

DELIMITER ;