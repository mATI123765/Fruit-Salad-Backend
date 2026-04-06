/* Function 2: This trigger automatically updates last_updated whenever a stat is modified */
USE new_super_idol_u;

DELIMITER //

CREATE TRIGGER tr_game_stats_before_update
BEFORE UPDATE ON game_stats
FOR EACH ROW
BEGIN
    -- Always update the timestamp when stat changes
    SET NEW.last_updated = NOW();
END //

DELIMITER ;