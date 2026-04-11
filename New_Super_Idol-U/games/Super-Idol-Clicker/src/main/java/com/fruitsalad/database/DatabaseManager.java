package com.fruitsalad.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =============================================================================
 * DATABASE MANAGER
 * =============================================================================
 * Handles all database operations for Super Idol Clicker.
 * Uses stored procedures and functions from the New Super Idol U database.
 * 
 * @author  Fruit Salad Ltd.
 * @version 1.0.0
 * =============================================================================
 */
public class DatabaseManager {
    
    // =========================================================================
    // CONSTANTS
    // =========================================================================
    
    /** Game ID for Super Idol Clicker in the videogame table */
    public static final int GAME_ID = 6;
    
    // =========================================================================
    // INSTANCE VARIABLES
    // =========================================================================
    
    private final Connection connection;
    
    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    
    /**
     * Creates a new DatabaseManager with an active connection.
     * 
     * @throws SQLException if connection cannot be established
     */
    public DatabaseManager() throws SQLException {
        this.connection = DatabaseConnection.getConnection();
    }
    
    // =========================================================================
    // USER OPERATIONS
    // =========================================================================
    
    /**
     * Validates user login credentials.
     * 
     * @param username The username to validate
     * @param passwordHash The hashed password
     * @return User ID if valid, -1 if invalid
     * @throws SQLException if database error occurs
     */
    public int loginUser(String username, String passwordHash) throws SQLException {
        String sql = "SELECT user_id FROM user WHERE username = ? AND password_hash = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("user_id");
                System.out.println("[Database] User logged in: " + username + " (ID: " + userId + ")");
                return userId;
            }
        }
        
        System.out.println("[Database] Login failed for: " + username);
        return -1;
    }
    
    /**
     * Gets username by user ID.
     * 
     * @param userId The user ID to look up
     * @return The username, or null if not found
     * @throws SQLException if database error occurs
     */
    public String getUsername(int userId) throws SQLException {
        String sql = "SELECT username FROM user WHERE user_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        }
        return null;
    }
    
    // =========================================================================
    // SESSION OPERATIONS
    // =========================================================================
    
    /**
     * Starts a new game session.
     * 
     * @param userId The user's ID
     * @return The new session ID
     * @throws SQLException if database error occurs
     */
    public int startGameSession(int userId) throws SQLException {
        String sql = "{CALL sp_start_game_session(?, ?, ?, ?)}";
        
        try (CallableStatement stmt = connection.prepareCall(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, GAME_ID);
            stmt.registerOutParameter(3, Types.INTEGER);
            stmt.registerOutParameter(4, Types.VARCHAR);
            
            stmt.execute();
            
            int sessionId = stmt.getInt(3);
            String result = stmt.getString(4);
            System.out.println("[Database] " + result);
            
            return sessionId;
        }
    }
    
    /**
     * Ends a game session.
     * 
     * @param sessionId The session to end
     * @return Duration in minutes
     * @throws SQLException if database error occurs
     */
    public int endGameSession(int sessionId) throws SQLException {
        String sql = "{CALL sp_end_game_session(?, ?, ?)}";
        
        try (CallableStatement stmt = connection.prepareCall(sql)) {
            stmt.setInt(1, sessionId);
            stmt.registerOutParameter(2, Types.INTEGER);
            stmt.registerOutParameter(3, Types.VARCHAR);
            
            stmt.execute();
            
            int duration = stmt.getInt(2);
            String result = stmt.getString(3);
            System.out.println("[Database] " + result);
            
            return duration;
        }
    }
    
    // =========================================================================
    // STATS OPERATIONS
    // =========================================================================
    
    /**
     * Records or updates a game stat.
     * 
     * @param userId User's ID
     * @param statName Name of the stat
     * @param value The value to set or add
     * @param mode "set" to replace, "add" to increment
     * @throws SQLException if database error occurs
     */
    public void recordStat(int userId, String statName, double value, String mode) throws SQLException {
        String sql = "{CALL sp_record_game_stat(?, ?, ?, ?, ?, ?, ?, ?)}";
        
        try (CallableStatement stmt = connection.prepareCall(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, GAME_ID);
            stmt.setString(3, statName);
            stmt.setDouble(4, value);
            stmt.setString(5, mode);
            stmt.registerOutParameter(6, Types.DECIMAL);
            stmt.registerOutParameter(7, Types.DECIMAL);
            stmt.registerOutParameter(8, Types.VARCHAR);
            
            stmt.execute();
        }
    }
    
    /**
     * Gets a specific stat value.
     * 
     * @param userId User's ID
     * @param statName Name of the stat
     * @return The stat value, or 0 if not found
     * @throws SQLException if database error occurs
     */
    public double getStat(int userId, String statName) throws SQLException {
        String sql = "SELECT fn_get_game_stat(?, ?, ?) AS stat_value";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, GAME_ID);
            stmt.setString(3, statName);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("stat_value");
            }
        }
        return 0.0;
    }
    
    /**
     * Gets all stats for a user in this game.
     * 
     * @param userId User's ID
     * @return Map of stat_name to stat_value
     * @throws SQLException if database error occurs
     */
    public Map<String, Double> getAllStats(int userId) throws SQLException {
        Map<String, Double> stats = new HashMap<>();
        String sql = "SELECT stat_name, stat_value FROM game_stats WHERE user_id = ? AND game_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, GAME_ID);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                stats.put(rs.getString("stat_name"), rs.getDouble("stat_value"));
            }
        }
        return stats;
    }
    
    // =========================================================================
    // ACHIEVEMENT OPERATIONS
    // =========================================================================
    
    /**
     * Unlocks an achievement for a user.
     * 
     * @param userId User's ID
     * @param achievementId Achievement to unlock
     * @return Result message
     * @throws SQLException if database error occurs
     */
    public String unlockAchievement(int userId, int achievementId) throws SQLException {
        String sql = "{CALL sp_unlock_achievement(?, ?, ?)}";
        
        try (CallableStatement stmt = connection.prepareCall(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, achievementId);
            stmt.registerOutParameter(3, Types.VARCHAR);
            
            stmt.execute();
            
            return stmt.getString(3);
        }
    }
    
    /**
     * Gets all achievements for this game.
     * 
     * @return List of achievement data maps
     * @throws SQLException if database error occurs
     */
    public List<Map<String, Object>> getGameAchievements() throws SQLException {
        List<Map<String, Object>> achievements = new ArrayList<>();
        String sql = "SELECT achievement_id, name, description, points, is_secret FROM achievement WHERE game_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, GAME_ID);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> achievement = new HashMap<>();
                achievement.put("id", rs.getInt("achievement_id"));
                achievement.put("name", rs.getString("name"));
                achievement.put("description", rs.getString("description"));
                achievement.put("points", rs.getInt("points"));
                achievement.put("isSecret", rs.getBoolean("is_secret"));
                achievements.add(achievement);
            }
        }
        return achievements;
    }
    
    /**
     * Gets IDs of achievements the user has unlocked.
     * 
     * @param userId User's ID
     * @return List of unlocked achievement IDs
     * @throws SQLException if database error occurs
     */
    public List<Integer> getUserAchievements(int userId) throws SQLException {
        List<Integer> unlockedIds = new ArrayList<>();
        String sql = """
            SELECT ua.achievement_id 
            FROM user_achievement ua
            INNER JOIN achievement a ON ua.achievement_id = a.achievement_id
            WHERE ua.user_id = ? AND a.game_id = ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, GAME_ID);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                unlockedIds.add(rs.getInt("achievement_id"));
            }
        }
        return unlockedIds;
    }
    
    /**
     * Gets achievement completion percentage.
     * 
     * @param userId User's ID
     * @return Percentage of achievements completed
     * @throws SQLException if database error occurs
     */
    public double getAchievementPercentage(int userId) throws SQLException {
        String sql = "SELECT fn_get_achievement_percentage(?, ?) AS percentage";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, GAME_ID);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("percentage");
            }
        }
        return 0.0;
    }
    
    // =========================================================================
    // PURCHASE OPERATIONS
    // =========================================================================
    
    /**
     * Records a purchase in the database.
     * 
     * @param userId User's ID
     * @param itemName Name of the purchased item
     * @param amount Cost of the item
     * @throws SQLException if database error occurs
     */
    public void recordPurchase(int userId, String itemName, double amount) throws SQLException {
        String sql = "INSERT INTO purchase (user_id, game_id, item_name, amount_spent, currency) VALUES (?, ?, ?, ?, 'CREDITS')";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, GAME_ID);
            stmt.setString(3, itemName);
            stmt.setDouble(4, amount);
            
            stmt.executeUpdate();
        }
    }
    
    // =========================================================================
    // PLAYTIME OPERATIONS
    // =========================================================================
    
    /**
     * Gets total playtime in hours.
     * 
     * @param userId User's ID
     * @return Total hours played
     * @throws SQLException if database error occurs
     */
    public double getTotalPlaytime(int userId) throws SQLException {
        String sql = "SELECT fn_get_total_playtime(?, ?) AS hours";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, GAME_ID);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("hours");
            }
        }
        return 0.0;
    }
    
    // =========================================================================
    // LEADERBOARD
    // =========================================================================
    
    /**
     * Gets top players by total credits.
     * 
     * @param limit Maximum number of entries to return
     * @return List of leaderboard entries
     * @throws SQLException if database error occurs
     */
    public List<Map<String, Object>> getLeaderboard(int limit) throws SQLException {
        List<Map<String, Object>> leaderboard = new ArrayList<>();
        String sql = """
            SELECT u.username, gs.stat_value as total_credits
            FROM game_stats gs
            INNER JOIN user u ON gs.user_id = u.user_id
            WHERE gs.game_id = ? AND gs.stat_name = 'total_credits_earned'
            ORDER BY gs.stat_value DESC
            LIMIT ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, GAME_ID);
            stmt.setInt(2, limit);
            
            ResultSet rs = stmt.executeQuery();
            int rank = 1;
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("rank", rank++);
                entry.put("username", rs.getString("username"));
                entry.put("totalCredits", rs.getDouble("total_credits"));
                leaderboard.add(entry);
            }
        }
        return leaderboard;
    }
    
    // =========================================================================
    // GAME SETUP
    // =========================================================================
    
    /**
     * Sets up Super Idol Clicker in the database.
     * Creates game entry and achievements if they don't exist.
     * 
     * @throws SQLException if database error occurs
     */
    public void setupGame() throws SQLException {
        // Check if game already exists
        String checkSql = "SELECT game_id FROM videogame WHERE game_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
            stmt.setInt(1, GAME_ID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("[Database] Game already exists (ID: " + GAME_ID + ")");
                return;
            }
        }
        
        // Insert the game
        String insertSql = """
            INSERT INTO videogame (game_id, title, genre, release_date, developer, publisher, cover_image_path)
            VALUES (?, 'Super Idol Clicker', 'Idle/Clicker', CURDATE(), 'Fruit Salad Ltd.', 'Fruit Salad Ltd.', '/images/cover.png')
            """;
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setInt(1, GAME_ID);
            stmt.executeUpdate();
            System.out.println("[Database] Game created successfully!");
        }
        
        setupAchievements();
    }
    
    /**
     * Creates achievements for Super Idol Clicker.
     */
    private void setupAchievements() throws SQLException {
        String sql = "INSERT IGNORE INTO achievement (game_id, name, description, points, is_secret) VALUES (?, ?, ?, ?, ?)";
        
        Object[][] achievements = {
            // Click achievements
            {GAME_ID, "First Click", "Click for the first time", 10, false},
            {GAME_ID, "100 Clicks", "Your smile is sweeter than Super Idol's", 20, false},
            {GAME_ID, "1000 Clicks", "Dedicated clicker", 50, false},
            {GAME_ID, "10000 Clicks", "The water is always cold with Super Idol", 100, false},
            {GAME_ID, "50000 Clicks", "You're a true Super Idol fan!", 170, false},
            {GAME_ID, "100000 Clicks", "Your heart is as pure as Super Idol's smile", 250, false},
            {GAME_ID, "1 Million Clicks", "Your love for Super Idol is immeasurable!", 500, true},
            
            // Credit achievements
            {GAME_ID, "+15 Social Credit", "Earn 100 credits", 10, false},
            {GAME_ID, "Good Citizen", "Earn 1,000 credits", 25, false},
            {GAME_ID, "Model Citizen", "Earn 10,000 credits", 50, false},
            {GAME_ID, "Super Idol's Favorite", "Earn 100,000 credits", 120, false},
            {GAME_ID, "Supreme Leader", "Earn 1,000,000 credits", 200, true},
            
            // Upgrade achievements
            {GAME_ID, "First Upgrade", "Buy your first upgrade", 15, false},
            {GAME_ID, "Upgrade Enthusiast", "Buy 10 upgrades", 30, false},
            
            // Playtime achievements
            {GAME_ID, "Loyal Fan", "Play for 1 hour total", 40, false},
            {GAME_ID, "Devoted Follower", "Play for 3 hours total", 80, false},
            {GAME_ID, "You gotta Love Super Idol", "Play for 10 hours total", 250, true}
        };
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Object[] achievement : achievements) {
                stmt.setInt(1, (int) achievement[0]);
                stmt.setString(2, (String) achievement[1]);
                stmt.setString(3, (String) achievement[2]);
                stmt.setInt(4, (int) achievement[3]);
                stmt.setBoolean(5, (boolean) achievement[4]);
                stmt.executeUpdate();
            }
        }
        System.out.println("[Database] Achievements configured!");
    }
}
