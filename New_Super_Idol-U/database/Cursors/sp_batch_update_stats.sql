/* Cursor 3: This procedure batch updates stats for all users in a game (useful for events like "Double XP Weekend"). */
USE new_super_idol_u;

DROP PROCEDURE IF EXISTS sp_batch_update_stats;

DELIMITER $$

CREATE PROCEDURE sp_batch_update_stats(
    IN p_game_id INT,
    IN p_stat_name VARCHAR(50),
    IN p_multiplier DECIMAL(5,2),
    OUT p_users_affected INT
)
BEGIN
    -- Variables for cursor
    DECLARE v_user_id INT;
    DECLARE v_current_value DECIMAL(15,2);
    DECLARE v_finished INT DEFAULT 0;
    DECLARE v_count INT DEFAULT 0;
    
    -- Declare cursor for users with this stat
    DECLARE stat_cursor CURSOR FOR 
        SELECT user_id, stat_value 
        FROM game_stats 
        WHERE game_id = p_game_id 
          AND stat_name = p_stat_name;
    
    -- Handler
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_finished = 1;
    
    -- Open cursor
    OPEN stat_cursor;
    
    -- Loop through all stats
    stat_loop: LOOP
        FETCH stat_cursor INTO v_user_id, v_current_value;
        
        IF v_finished = 1 THEN
            LEAVE stat_loop;
        END IF;
        
        -- Update the stat with multiplier
        UPDATE game_stats 
        SET stat_value = v_current_value * p_multiplier
        WHERE user_id = v_user_id 
          AND game_id = p_game_id 
          AND stat_name = p_stat_name;
        
        SET v_count = v_count + 1;
    END LOOP;
    
    -- Close cursor
    CLOSE stat_cursor;
    
    -- Return count
    SET p_users_affected = v_count;
END$$

DELIMITER ;