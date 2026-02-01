/* Trigger 1: This trigger automatically calculates duration when a session ends */
USE new_super_idol_u;

DELIMITER //

CREATE TRIGGER tr_playtime_before_update
BEFORE UPDATE ON playtime
FOR EACH ROW
BEGIN
    -- If end_time is being set (was NULL, now has value)
    IF OLD.end_time IS NULL AND NEW.end_time IS NOT NULL THEN
        -- Validate that end_time is after start_time
        IF NEW.end_time < NEW.start_time THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ERROR: end_time cannot be before start_time';
        END IF;
    END IF;
END //

DELIMITER ;