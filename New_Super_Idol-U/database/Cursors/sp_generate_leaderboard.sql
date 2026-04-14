/* Cursor 2: This procedure generates a leaderboard for a specific game, ranking users by achievement completion. */
USE new_super_idol_u;

DROP PROCEDURE IF EXISTS sp_generate_leaderboard;

DELIMITER $$

CREATE PROCEDURE sp_generate_leaderboard(
    IN p_game_id INT
)
BEGIN
    -- Variables for cursor data
    DECLARE v_user_id INT;
    DECLARE v_username VARCHAR(50);
    DECLARE v_finished INT DEFAULT 0;
    
    -- Variables for calculations
    DECLARE v_playtime DECIMAL(10,2);
    DECLARE v_achievement_pct DECIMAL(5,2);
    DECLARE v_collection_pct DECIMAL(5,2);
    DECLARE v_game_title VARCHAR(150);
    DECLARE v_rank INT DEFAULT 0;
    
    -- Declare cursor
    DECLARE user_cursor CURSOR FOR 
        SELECT user_id, username FROM user;
    
    -- Handler
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_finished = 1;
    
    -- Get game title
    SELECT title INTO v_game_title 
    FROM videogame 
    WHERE game_id = p_game_id;
    
    -- Create temporary table for leaderboard
    DROP TEMPORARY TABLE IF EXISTS temp_leaderboard;
    CREATE TEMPORARY TABLE temp_leaderboard (
        rank_position INT,
        username VARCHAR(50),
        hours_played DECIMAL(10,2),
        achievement_pct DECIMAL(5,2),
        collection_pct DECIMAL(5,2),
        total_score DECIMAL(10,2)
    );
    
    -- Open cursor
    OPEN user_cursor;
    
    -- Loop through all users
    user_loop: LOOP
        FETCH user_cursor INTO v_user_id, v_username;
        
        IF v_finished = 1 THEN
            LEAVE user_loop;
        END IF;
        
        -- Calculate stats
        SET v_playtime = fn_get_total_playtime(v_user_id, p_game_id);
        SET v_achievement_pct = fn_get_achievement_percentage(v_user_id, p_game_id);
        SET v_collection_pct = fn_get_collection_percentage(v_user_id, p_game_id);
        
        -- Only include users who have played this game
        IF v_playtime > 0 OR v_achievement_pct > 0 THEN
            INSERT INTO temp_leaderboard (
                rank_position,
                username, 
                hours_played, 
                achievement_pct, 
                collection_pct,
                total_score
            ) VALUES (
                0,  -- Will update rank later
                v_username,
                v_playtime,
                v_achievement_pct,
                v_collection_pct,
                -- Score formula: achievements worth most, then collectables, then playtime bonus
                (v_achievement_pct * 10) + (v_collection_pct * 5) + (v_playtime * 0.5)
            );
        END IF;
    END LOOP;
    
    -- Close cursor
    CLOSE user_cursor;
    
    -- Update rankings based on total_score
    SET v_rank = 0;
    UPDATE temp_leaderboard 
    SET rank_position = (@row_num := @row_num + 1)
    ORDER BY total_score DESC;
    
    -- Alternative for this: Use a second cursor or ROW_NUMBER (MySQL 8+)
    -- For simplicity, we'll just order the output
    
    -- Show leaderboard header
    SELECT CONCAT('===== LEADERBOARD: ', v_game_title, ' =====') AS leaderboard_header;
    
    -- Show leaderboard (ordered by score)
    SELECT 
        ROW_NUMBER() OVER (ORDER BY total_score DESC) AS `rank`,
        username,
        hours_played,
        CONCAT(achievement_pct, '%') AS achievements,
        CONCAT(collection_pct, '%') AS collectables,
        ROUND(total_score, 2) AS score
    FROM temp_leaderboard
    ORDER BY total_score DESC;
    
    -- Cleanup
    DROP TEMPORARY TABLE IF EXISTS temp_leaderboard;
END$$

DELIMITER ;