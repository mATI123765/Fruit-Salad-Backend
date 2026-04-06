/* Procedure 2: This procedure starts a new gaming session for a user*/
USE new_super_idol_u;

DELIMITER //

CREATE PROCEDURE sp_start_game_session(
    IN p_user_id INT,
    IN p_game_id INT,
    OUT p_session_id INT,
    OUT p_result VARCHAR(100)
)
BEGIN
    -- Declare variables
    DECLARE v_user_exists INT DEFAULT 0;
    DECLARE v_game_exists INT DEFAULT 0;

    -- Check if user exists
    SELECT COUNT(*) INTO v_user_exists
    FROM user
    WHERE user_id = p_user_id;

    -- Check if game exists
    SELECT COUNT(*) INTO v_game_exists
    FROM videogame
    WHERE game_id = p_game_id;

    -- Handle cases based on existence checks
    IF v_user_exists = 0 THEN
        SET p_session_id = NULL;
        SET p_result = 'ERROR: User does not exist';
    ELSEIF v_game_exists = 0 THEN
        SET p_session_id = NULL;
        SET p_result = 'ERROR: Game does not exist';
    ELSE
        -- Insert new session with start_time
        INSERT INTO playtime (user_id, game_id, start_time)
        VALUES (p_user_id, p_game_id, NOW());

        -- Get the generated session_id
        SET p_session_id = LAST_INSERT_ID();

        SET p_result = CONCAT('SUCCESS: Session started! Session ID: ', p_session_id);
    END IF;
END //

DELIMITER ;