/* Procedure 5: This procedure updates the status of a side quest for a user */
USE new_super_idol_u;

DELIMITER //

CREATE PROCEDURE sp_update_quest_status(
    IN p_user_id INT,
    IN p_quest_id INT,
    IN p_new_status ENUM('not_started', 'in_progress', 'completed'),
    OUT p_quest_name VARCHAR(100),
    OUT p_reward VARCHAR(255),
    OUT p_result(150)
)
BEGIN
    -- Declare variables
    DECLARE v_user_exists INT DEFAULT 0;
    DECLARE v_quest_exists INT DEFAULT 0;
    DECLARE v_record_exists INT DEFAULT 0;
    DECLARE v_current_status VARCHAR(20);

    -- Check if user exists
    SELECT COUNT(*) INTO v_user_exists
    FROM user
    WHERE user_id = p_user_id;

    -- Check if quest exists and get info
    SELECT COUNT(*) INTO v_quest_exists
    FROM side_quest
    WHERE quest_id = p_quest_id;

    -- Handle errors
    IF v_user_exists = 0 THEN
        SET p_quest_name = NULL;
        SET p_reward = NULL;
        SET p_resutl = 'ERROR: User does not exist';
    ELSEIF v_quest_exists = 0 THEN
        SET p_quest_name = NULL;
        SET p_reward = NULL;
        SET p_result = 'ERROR: Quest does not exist';
    ELSE
        -- Get quest info
        SELECT name, reward INTO p_quest_name, p_reward
        FROM side_quest
        WHERE quest_id = p_quest_id;

        -- Check if user_quest record exists
        SELECT COUNT(*) INTO v_record_exists
        FROM user_quest
        WHERE user_id = p_user_id AND quest_id = p_quest_id;

        IF v_record_exists = 0 THEN
            -- Create a new record
            INSERT INTO user_quest (user_id, quest_id, status, completed_at)
            VALUES (
                p_user_id,
                p_quest_id,
                p_new_status,
                IF(p_new_status = 'completed', NOW(), NULL)
            );

            SET p_result = CONCAT('SUCCESS: Quest "', p_quest_name, '" set to ', UPPER(p_new_status));
        ELSE
            -- Update existing record
            UPDATE user_quest
            SET
                status = p_new_status,
                completed_at = IF(p_new_status = 'completed', NOW(), completed_at)
            WHERE user_id = p_user_id AND quest_id = p_quest_id;

            SET p_result = CONCAT('SUCCESS: Quest "', p_quest_name, '" updated to', UPPER(p_new_status));
        END IF;
    END IF;
END //

DELIMITER ;