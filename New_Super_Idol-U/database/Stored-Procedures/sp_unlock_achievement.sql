/* Procedure 1: This procedure unlocks an achievement for a user */
USE new_super_idol_u;

DROP PROCEDURE IF EXISTS sp_unlock_achievement;

DELIMITER $$

CREATE PROCEDURE sp_unlock_achievement(
    IN p_user_id INT,
    IN p_achievement_id INT,
    OUT p_result VARCHAR(100)
)
BEGIN
    -- Declare variables
    DECLARE v_user_exists INT DEFAULT 0;
    DECLARE v_achievement_exists INT DEFAULT 0;
    DECLARE v_already_unlocked INT DEFAULT 0;

    -- Check if user exists
    SELECT COUNT(*) INTO v_user_exists
    FROM user
    WHERE user_id = p_user_id;

    -- Check if achievement exists
    SELECT COUNT(*) INTO v_achievement_exists
    FROM achievement
    WHERE achievement_id = p_achievement_id;

    -- Check if already unlocked
    SELECT COUNT(*) INTO v_already_unlocked
    FROM user_achievement
    WHERE user_id = p_user_id AND achievement_id = p_achievement_id;

    -- Logic to unlock achievement
    IF v_user_exists = 0 THEN
        SET p_result = 'ERROR: User does not exist';
    ELSEIF v_achievement_exists = 0 THEN
        SET p_result = 'ERROR: Achievement does not exists';
    ELSEIF v_already_unlocked > 0 THEN
        SET p_result = 'WARNING: Achievement already unlocked';
    ELSE
        -- Insert the achievement
        INSERT INTO user_achievement (user_id, achievement_id, unlocked_at)
        VALUES (p_user_id, p_achievement_id, NOW());

        SET p_result = 'SUCCESS: Achievement unlocked!';
    END IF;
END$$

DELIMITER ;