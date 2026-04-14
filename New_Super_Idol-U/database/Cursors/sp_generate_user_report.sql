/* Cursor 1: This procedure generates a complete gaming report for a specific user, showing all games with their stats. */
USE new_super_idol_u;

DROP PROCEDURE IF EXISTS sp_generate_user_report;

DELIMITER $$

CREATE PROCEDURE sp_generate_user_report(
    IN p_user_id INT
)
BEGIN
    -- Variables for cursor data
    DECLARE v_game_id INT;
    DECLARE v_game_title VARCHAR(150);
    DECLARE v_finished INT DEFAULT 0;
    
    -- Variables for calculations
    DECLARE v_playtime DECIMAL(10,2);
    DECLARE v_achievement_pct DECIMAL(5,2);
    DECLARE v_collection_pct DECIMAL(5,2);
    DECLARE v_spent DECIMAL(10,2);
    DECLARE v_username VARCHAR(50);
    
    -- Declare cursor
    DECLARE game_cursor CURSOR FOR 
        SELECT game_id, title FROM videogame;
    
    -- Handler for end of cursor
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_finished = 1;
    
    -- Get username
    SELECT username INTO v_username 
    FROM user 
    WHERE user_id = p_user_id;
    
    -- Create temporary table for report
    DROP TEMPORARY TABLE IF EXISTS temp_user_report;
    CREATE TEMPORARY TABLE temp_user_report (
        game_title VARCHAR(150),
        hours_played DECIMAL(10,2),
        achievement_completion DECIMAL(5,2),
        collection_completion DECIMAL(5,2),
        money_spent DECIMAL(10,2)
    );
    
    -- Open cursor
    OPEN game_cursor;
    
    -- Loop through all games
    game_loop: LOOP
        FETCH game_cursor INTO v_game_id, v_game_title;
        
        IF v_finished = 1 THEN
            LEAVE game_loop;
        END IF;
        
        -- Calculate stats using our functions
        SET v_playtime = fn_get_total_playtime(p_user_id, v_game_id);
        SET v_achievement_pct = fn_get_achievement_percentage(p_user_id, v_game_id);
        SET v_collection_pct = fn_get_collection_percentage(p_user_id, v_game_id);
        SET v_spent = fn_get_total_spent(p_user_id, v_game_id);
        
        -- Insert into report (only if user has activity)
        IF v_playtime > 0 OR v_achievement_pct > 0 OR v_spent > 0 THEN
            INSERT INTO temp_user_report VALUES (
                v_game_title,
                v_playtime,
                v_achievement_pct,
                v_collection_pct,
                v_spent
            );
        END IF;
    END LOOP;
    
    -- Close cursor
    CLOSE game_cursor;
    
    -- Show the report header
    SELECT CONCAT('===== GAMING REPORT FOR: ', v_username, ' =====') AS report_header;
    
    -- Show the report data
    SELECT * FROM temp_user_report;
    
    -- Show totals
    SELECT 
        SUM(hours_played) AS total_hours,
        AVG(achievement_completion) AS avg_achievement_pct,
        AVG(collection_completion) AS avg_collection_pct,
        SUM(money_spent) AS total_spent
    FROM temp_user_report;
    
    -- Cleanup
    DROP TEMPORARY TABLE IF EXISTS temp_user_report;
END$$

DELIMITER ;