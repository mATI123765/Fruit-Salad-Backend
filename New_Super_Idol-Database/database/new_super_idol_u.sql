-- ============================================
-- NEW SUPER IDOL U - Videogame Database
-- Intermodular Project by Fruit Salad Ltd.
-- ============================================
-- Authors: Jorge Ferrando, Joel Acosta, Iker Mollá, Zakaria Hdouri
-- Date: January 2026
-- ============================================

-- Create the database and use it
DROP DATABASE IF EXISTS new_super_idol_u;
CREATE DATABASE new_super_idol_u CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE new_super_idol_u;

--------------------------
/* CREATE MAIN ENTITIES */
--------------------------

-- USER table: The player
CREATE TABLE user (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP

    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB;

-- VIDEOGAME table: Game information
CREATE TABLE videogame (
    game_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    genre VARCHAR(50),
    release_date DATE,
    developer VARCHAR(100),
    publisher VARCHAR(100),
    cover_image_path VARCHAR(255),

    INDEX idx_title (title),
    INDEX idx_genre (genre)
) ENGINE=InnoDB;

-- ACHIEVEMENT table: Game achievements
CREATE TABLE achievement(
    achievement_id INT PRIMARY KEY AUTO_INCREMENT,
    game_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    points INT DEFAULT 0,
    icon_path VARCHAR(255),
    is_secret BOOLEAN DEFAULT FALSE,

    INDEX idx_game_id (game_id),
    CONSTRAINT fk_achievement_game
        Foreign Key (game_id) REFERENCES videogame(game_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- COLLECTABLE table: In-game collectables
CREATE TABLE collectable (
    collectable_id INT PRIMARY KEY AUTO_INCREMENT,
    game_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    rarity ENUM('common', 'rare', 'epic', 'legendary') DEFAULT 'common',
    location_hint VARCHAR(255),

    INDEX idx_game_id (game_id),
    INDEX idx_rarity (rarity),
    CONSTRAINT fk_collectable_game
        Foreign Key (game_id) REFERENCES videogame(game_id)
        ON DELETE CASCADE ON UPDATE CASCADE
)ENGINE=InnoDB;

-- SIDE_QUEST table: Side quests in games
CREATE TABLE side_quest (
    quest_id INT PRIMARY KEY AUTO_INCREMENT,
    game_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    reward VARCHAR(255),
    difficulty ENUM('easy', 'medium', 'hard', 'extreme') DEFAULT 'medium',

    INDEX idx_game_id (game_id),
    INDEX idx_difficulty (difficulty),
    CONSTRAINT fk_side_quest_game
        Foreign Key (game_id) REFERENCES videogame(game_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-----------------------------------------
/* RELATIONSHIP TABLES -- Many-To-Many */
-----------------------------------------

-- USER_ACHIEVEMENT: Links users to their unlocked achievements
CREATE TABLE user_achievement (
    user_id INT NOT NULL,
    achievement_id INT NOT NULL,
    unlocked_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, achievement_id),
    INDEX idx_user_id (user_id),
    INDEX idx_achievement_id (achievement_id),
    CONSTRAINT fk_user_achievement_user
        Foreign Key (user_id) REFERENCES user(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_achievement_achievement
        Foreign Key (achievement_id) REFERENCES achievement(achievement_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- USER_COLLECTABLE: Links users to their collected items
CREATE TABLE user_collectable (
    user_id INT NOT NULL,
    collectable_id INT NOT NULL,
    collected_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, collectable_id),
    INDEX idx_user_id (user_id),
    INDEX idx_collectable_id (collectable_id),
    CONSTRAINT fk_user_collectable_user
        Foreign Key (user_id) REFERENCES user(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_collectable_collectable
        Foreign Key (collectable_id) REFERENCES collectable(collectable_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- USER_QUEST: Links users to their quest progress
CREATE TABLE user_quest (
    user_id INT NOT NULL,
    quest_id INT NOT NULL,
    status ENUM('not_started', 'in_progress', 'completed') DEFAULT 'not_started',
    completed_at DATETIME NULL,

    PRIMARY KEY (user_id, quest_id),
    INDEX idx_user_id (user_id),
    INDEX idx_quest_id (quest_id),
    INDEX idx_status (status),
    CONSTRAINT fk_user_quest_user
        Foreign Key (user_id) REFERENCES user(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_quest_quest
        Foreign Key (quest_id) REFERENCES side_quest(quest_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-----------------------------------------------
/* TRACKING TABLE -- User-Game specific data */
-----------------------------------------------

-- PLAYTIME: Session tracking for each user/game
CREATE TABLE playtime (
    session_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    game_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    duration_minutes INT GENERATED ALWAYS AS (TIMESTAMPDIFF(MINUTE, start_time, end_time)) STORED,

    INDEX idx_user_id (user_id),
    INDEX idx_game_id (game_id),
    INDEX idx_start_time (start_time),
    CONSTRAINT fk_playtime_user
        Foreign Key (user_id) REFERENCES user(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_playtime_game
        Foreign Key (game_id) REFERENCES videogame(game_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- GAME_STATS: Flexible stats tracking (money, kill, deaths, etc.)
CREATE TABLE game_stats (
    stat_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    game_id INT NOT NULL,
    stat_name VARCHAR(50) NOT NULL,
    stat_value DECIMAL(15, 2) DEFAULT 0,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY unique_user_game_stat (user_id, game_id, stat_name),
    INDEX idx_user_id (user_id),
    INDEX idx_game_id (game_id),
    INDEX idx_stat_name (stat_name),
    CONSTRAINT fk_game_stats_user
        Foreign Key (user_id) REFERENCES user(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_game_stats_game
        Foreign Key (game_id) REFERENCES videogame(game_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- SAVE_FILE: Backup management for save files
CREATE TABLE save_file (
    save_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    game_id INT NOT NULL,
    save_name VARCHAR(100) NOT NULL,
    file_path VARCHAR(256) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    file_size_bytes BIGINT DEFAULT 0,

    INDEX idx_user_id (user_id),
    INDEX idx_game_id (game_id),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_save_file_user
        Foreign Key (user_id) REFERENCES user(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_save_file_game
        Foreign Key (game_id) REFERENCES videogame(game_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- PURCHASE: Money spent tracking
CREATE TABLE purchase (
    purchase_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    game_id INT NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    amount_spent DECIMAL(3) DEFAULT 'EUR',
    purchase_date DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_game_id (game_id),
    INDEX idx_purchase_date (purchase_game),
    CONSTRAINT fk_purchase_user
        Foreign Key (user_id) REFERENCES user(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_purchase_game
        Foreign Key (game_id) REFERENCES videogame(game_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

--------------------
/* CREATING VIEWS */
--------------------

-- View 1: Total playtime per user per game
CREATE VIEW v_user_total_playtime AS
SELECT
    u.user_id,
    u.username,
    v.game_id,
    v.title AS game title,
    COUNT(p.session_id) AS total_sessions,
    COALESCE(SUM(p.duration_minutes), 0) AS total_minutes,
    ROUND(COALESCE(SUM(p.duration_minutes), 0) / 60, 2) AS total_hours -- Convert minutes to hours
FROM user u
CROSS JOIN videogame v
LEFT JOIN playtime p ON u.user_id = p.user_id AND v.game_id = p.game_id
GROUP BY u.user_id, u.username, v.game_id, v.title;

-- View 2: Achievement completion percentage per user per game
CREATE VIEW v_user_achievement_progress AS
SELECT
    u.user_id,
    u.username,
    v.game_id,
    v.title AS game_title,
    COUNT(DISTINCT a.achievement_id) AS total_achievements,
    COUNT(DISTINCT ua.achievement_id) AS unlocked_achievements,
    ROUND(
        (COUNT(DISTINCT ua.achievement_id) / NULLIF(COUNT(DISTINCT a.achievement_id), 0)) * 100, -- Avoid division by zero
        2
    ) AS completion_percentage
FROM user u
CROSS JOIN videogame v
LEFT JOIN achievement a ON v.game_id = a.game_id
LEFT JOIN user_achievement ua ON u.user_id = ua.user_id AND a.achievement_id = ua.achievement_id
GROUP BY u.user_id, u.username, v.game_id, v.title;

-- View 3: Total money spent per user
CREATE VIEW v_user_total_spent AS
SELECT
    u.user_id,
    u.username,
    v.game_id,
    v.title AS game_title,
    COUNT(p.purchase_id) AS total_purchases,
    COALESCE(SUM(p.amount_spent), 0) AS total_spent,
    p.currency -- Assuming currency is stored in purchase table
FROM user u
CROSS JOIN videogame v
LEFT JOIN purchase p ON u.user_id = p.user_id AND v.game_id = p.game_id
GROUP BY u.user_id, u.username, v.game_id, v.title, p.currency;