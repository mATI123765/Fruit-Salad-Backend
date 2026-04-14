/* Procedure 6: This procedure records or updates a game stadistic for a user */
USE new_super_idol_u;

DROP PROCEDURE IF EXISTS sp_record_game_stat;

DELIMITER $$

CREATE PROCEDURE sp_record_game_stat(
    IN p_user_id INT,
    IN p_game_id INT,
    IN p_stat_name VARCHAR(60),
    IN p_stat_value DECIMAL(15,2),
    IN p_mode ENUM('set', 'add'),
    OUT p_old_value DECIMAL(15,2),
    OUT p_new_value DECIMAL(15,2),
    OUT p_result VARCHAR(150)
)
BEGIN
    -- Declare variables
    DECLARE v_user_exists INT DEFAULT 0;
    DECLARE v_game_exists INT DEFAULT 0;
    DECLARE v_stat_exists INT DEFAULT 0;
    DECLARE v_current_value DECIMAL(15,2) DEFAULT 0;

    -- Check if user exists
    SELECT COUNT(*) INTO v_user_exists
    FROM user
    WHERE user_id = p_user_id;

    -- Check if game exists
    SELECT COUNT(*) INTO v_game_exists 
    FROM videogame 
    WHERE game_id = p_game_id;

    -- Handle errors for non-existing user or game
    IF v_user_exists = 0 THEN
        SET p_old_value = NULL;
        SET p_new_value = NULL;
        SET p_result = 'ERROR: User does not exist';
    ELSEIF v_game_exists = 0 THEN
        SET p_old_value = NULL;
        SET p_new_value = NULL;
        SET p_result = 'ERROR: Game does not exist';
    ELSE
        -- Check if stat already exists
        SELECT COUNT(*), COALESCE(stat_value, 0)
        INTO v_stat_exists, v_current_value
        FROM game_stats
        WHERE user_id = p_user_id
            AND game_id = p_game_id
            AND stat_name = p_stat_name;

        -- Stored old value
        SET p_old_value = v_current_value;

        -- Calculate new value based on mode
        IF p_mode = 'set' THEN
            -- SET mode: replace the value
            SET p_new_value = p_stat_value;
        ELSE
            -- ADD mode: add to current value
            SET p_new_value = v_current_value + p_stat_value;
        END IF;
        
        IF v_stat_exists = 0 THEN
            -- Insert new stat
            INSERT INTO game_stats (user_id, game_id, stat_name, stat_value)
            VALUES (p_user_id, p_game_id, p_stat_name, p_new_value);
            
            SET p_result = CONCAT('SUCCESS: New stat "', p_stat_name, '" created = ', p_new_value);
        ELSE
            -- Update existing stat
            UPDATE game_stats 
            SET stat_value = p_new_value
            WHERE user_id = p_user_id 
                AND game_id = p_game_id 
                AND stat_name = p_stat_name;
            
            IF p_mode = 'add' THEN
                SET p_result = CONCAT('SUCCESS: "', p_stat_name, '" updated: ', p_old_value, ' + ', p_stat_value, ' = ', p_new_value);
            ELSE
                SET p_result = CONCAT('SUCCESS: "', p_stat_name, '" changed: ', p_old_value, ' → ', p_new_value);
            END IF;
        END IF;
    END IF;
END$$

DELIMITER ;