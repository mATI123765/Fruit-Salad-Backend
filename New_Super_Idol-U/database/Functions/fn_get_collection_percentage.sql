/* Function 5: This function returns the collectable completion percentage for a user in a specific game */
USE new_super_idol_u;

DROP FUNCTION IF EXISTS fn_get_collection_percentage;

DELIMITER $$

CREATE FUNCTION fn_get_collection_percentage(
    p_user_id INT,
    p_game_id INT
)
RETURNS DECIMAL(5,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total_collectables INT DEFAULT 0;
    DECLARE v_collected INT DEFAULT 0;

    SELECT COUNT(*) INTO v_total_collectables
    FROM collectable
    WHERE game_id = p_game_id;

    SELECT COUNT(*) INTO v_collected
    FROM user_collectable uc
    INNER JOIN collectable c ON uc.collectable_id = c.collectable_id
    WHERE uc.user_id = p_user_id
      AND c.game_id = p_game_id;

    IF v_total_collectables = 0 THEN
        RETURN 0.00;
    END IF;

    RETURN ROUND((v_collected / v_total_collectables) * 100, 2);
END$$

DELIMITER ;