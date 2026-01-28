/* Procedure 3: This procedure ends a gaming session */
USE new_super_idol_u;

DELIMITER //

CREATE PROCEDURE sp_end_game_session(
    IN p_session_id INT,
    OUT p_duration_minutes INT,
    OUT p_result VARCHAR(100)
)
BEGIN
    -- Declare variables
    DECLARE v_session_exists INT DEFAULT 0;
    DECLARE v_already_ended INT DEFAULT 0;
    DECLARE v_start_time DATETIME;

    -- Check if session exists
    SELECT COUNT(*) INTO v_session_exists
    FROM playtime
    WHERE session_id = p_session_id;

    -- Check if session already has an end_time
    SELECT COUNT(*) INTO v_already_ended
    FROM playtime
    WHERE session_id = p_session_id AND end_time IS NOT NULL;

    -- Handle different scenarios
    IF v_session_exists = 0 THEN
        SET p_duration_minutes = NULL;
        SET p_result = 'ERROR: Session does not exist';
    ELSEIF v_already_ended > 0 THEN
        SET p_duration_minutes = NULL;
        SET p_result = 'WARNING: Session already ended';
    ELSE
        -- Update the session with end_time
        UPDATE playtime
        SET end_time = NOW()
        WHERE session_id = p_session_id;

        -- Get the calculated duration
        SELECT duration_minutes INTO p_duration_minutes
        FROM playtime
        WHERE session_id = p_session_id;

        SET p_result = CONCAT('SUCCESS: Session ended! Duration: ', p_duration_minutes, 'minutes');
    END IF;
END //

DELIMITER ;