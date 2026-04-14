/* Procedure 4: This procedure adds a collectable to a user's inventory */
USE new_super_idol_u;

DROP PROCEDURE IF EXISTS sp_collect_item;

DELIMITER $$

CREATE PROCEDURE sp_collect_item(
    IN p_user_id INT,
    IN p_collectable_id INT,
    OUT p_item_name VARCHAR(100),
    OUT p_rarity VARCHAR(20),
    OUT p_result VARCHAR(100)
)
BEGIN
    -- Declare variables
    DECLARE v_user_exists INT DEFAULT 0;
    DECLARE v_collectable_exists INT DEFAULT 0;
    DECLARE v_already_collected INT DEFAULT 0;

    -- Check if user exists
    SELECT COUNT(*) INTO v_user_exists
    FROM user
    WHERE user_id = p_user_id;

    -- Check if collectable exists and get its info
    SELECT COUNT(*) INTO v_collectable_exists
    FROM collectable
    WHERE collectable_id = p_collectable_id;

    -- Check if already collected
    SELECT COUNT(*) INTO v_already_collected
    FROM user_collectable
    WHERE user_id = p_user_id AND collectable_id = p_collectable_id;

    -- Handle different scenarios
    IF v_user_exists = 0 THEN
        SET p_item_name = NULL;
        SET p_rarity = NULL;
        SET p_result = 'ERROR: User does not exist';
    ELSEIF v_collectable_exists = 0 THEN
        SET p_item_name = NULL;
        SET p_rarity = NULL;
        SET p_result = 'ERROR: Collectable does not exist';
    ELSEIF v_already_collected > 0 THEN
        -- Get item info even if already collected
        SELECT name, rarity INTO p_item_name, p_rarity
        FROM collectable
        WHERE collectable_id = p_collectable_id;

        SET p_result = CONCAT('WARNING: You already have "', p_item_name, '"');
    ELSE
        -- Get item info
        SELECT name, rarity INTO p_item_name, p_rarity
        FROM collectable
        WHERE collectable_id = p_collectable_id;

        -- Insert the collectable
        INSERT INTO user_collectable (user_id, collectable_id, collected_at)
        VALUES (p_user_id, p_collectable_id, NOW());

        SET p_result = CONCAT('SUCCESS: Collected "', p_item_name, '" (', UPPER(p_rarity), ')');
    END IF;
END$$

DELIMITER ;