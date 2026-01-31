/* Function 4: This function returns a specific stat value for a user in a game */
USE new_super_idol_u;

DELIMITER //

CREATE FUNCTION fn_get_game_stat(
    p_user_id INT,
    p_game_id INT,
    p_stat_name VARCHAR(50)
)
RETURNS DECIMAL(15,2) -- Assuming stats are decimal values
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_stat_value DECIMAL (15,2) DEFAULT 0.00;

    SELECT COALESCE(stat_value, 0.00) INTO v_stat_value
    FROM game_stats
    WHERE user_id = p_user_id
        AND game_id = p_game_id
        AND stat_name = p_stat_name;
    
    RETURN v_stat_value;
END //

DELIMITER ;