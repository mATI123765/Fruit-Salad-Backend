/* Trigger 3: This trigger validates achievement unlocks - prevents unlocking achievements for games
the user doesn't own (if you add a user_videogame ownership table later) and prevents
duplicate unlocks. */
USE new_super_idol_u;

DELIMITER //

CREATE TRIGGER tr_achievement_before_insert
BEFORE INSERT ON user_achievement
FOR EACH ROW
BEGIN
    DECLARE v_already_exists INT DEFAULT 0;
    DECLARE v_achievement_name VARCHAR(100);

    -- Check if already unlocked
    SELECT COUNT(*) INTO v_already_exists
    FROM user_achievement
    WHERE user_id = NEW.user_id
        AND achievement_id = NEW.achievement_id;

    IF v_already_exists > 0 THEN
        SELECT name INTO v_achievement_name
        FROM achievement
        WHERE achievement_id = NEW.achievement_id;

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'ERROR: Achievement already unlocked';
    END IF;

    -- Auto-set unlocked_at if not provided
    IF NEW.unlocked_at IS NULL THEN
        SET NEW.unlocked_at = NOW();
    END IF;
END //

DELIMITER ;