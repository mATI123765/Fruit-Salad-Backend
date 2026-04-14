/* Function 3: This function returns the total money spent by a user */
USE new_super_idol_u;

DROP FUNCTION IF EXISTS fn_get_total_spent;

DELIMITER $$

CREATE FUNCTION fn_get_total_spent(
    p_user_id INT,
    p_game_id INT
)
RETURNS DECIMAL(10,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total DECIMAL(10,2) DEFAULT 0.00;

    IF p_game_id IS NULL THEN
        -- Total spent on ALL games
        SELECT COALESCE(SUM(amount_spent), 0.00) INTO v_total
        FROM purchase
        WHERE user_id = p_user_id;
    ELSE
        -- Total spent on specific game
        SELECT COALESCE(SUM(amount_spent), 0.00) INTO v_total
        FROM purchase
        WHERE user_id = p_user_id
            AND game_id = p_game_id;
    END IF;

    RETURN v_total;
END$$

DELIMITER ;