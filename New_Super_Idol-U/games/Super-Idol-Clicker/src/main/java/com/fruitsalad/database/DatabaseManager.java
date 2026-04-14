package com.fruitsalad.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =============================================================================
 * DATABASE MANAGER
 * =============================================================================
 * Handles all database operations for Super Idol Clicker.
 * Uses simple SQL queries for compatibility.
 * 
 * @author  Fruit Salad Ltd.
 * @version 1.0.0
 * =============================================================================
 */
public class DatabaseManager {
    
    public static final int GAME_ID = 6;
    private final Connection connection;
    
    public DatabaseManager() throws SQLException { this.connection = DatabaseConnection.getConnection(); }
    
    /* USER OPERATIONS */
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
        return -1;
    }
    
    /* SESSION OPERATIONS */    
    public int startGameSession(int userId) throws SQLException {
        String sql = "INSERT INTO playtime (user_id, game_id, start_time) VALUES (?, ?, NOW())";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, GAME_ID);
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int sessionId = rs.getInt(1);
                System.out.println("[Database] Session started: " + sessionId);
                return sessionId;
            }
        }
        return -1;
    }
    
    public void endGameSession(int sessionId) throws SQLException {
        String sql = "UPDATE playtime SET end_time = NOW() WHERE playtime_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);
            stmt.executeUpdate();
            System.out.println("[Database] Session ended: " + sessionId);
        }
    }
    
    /* STATS OPERATIONS */
    public void recordStat(int userId, String statName, double value, String mode) throws SQLException {
        String checkSql = "SELECT stat_value FROM game_stats WHERE user_id = ? AND game_id = ? AND stat_name = ?";
        
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, GAME_ID);
            checkStmt.setString(3, statName);
            
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                double currentValue = rs.getDouble("stat_value");
                double newValue = mode.equals("add") ? currentValue + value : value;
                
                String updateSql = "UPDATE game_stats SET stat_value = ?, last_updated = NOW() WHERE user_id = ? AND game_id = ? AND stat_name = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setDouble(1, newValue);
                    updateStmt.setInt(2, userId);
                    updateStmt.setInt(3, GAME_ID);
                    updateStmt.setString(4, statName);
                    updateStmt.executeUpdate();
                }
            } else {
                String insertSql = "INSERT INTO game_stats (user_id, game_id, stat_name, stat_value) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setInt(2, GAME_ID);
                    insertStmt.setString(3, statName);
                    insertStmt.setDouble(4, value);
                    insertStmt.executeUpdate();
                }
            }
        }
    }
    
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
    
    /* ACHIEVEMENT OPERATIONS */
    public String unlockAchievement(int userId, int achievementId) throws SQLException {
        String checkSql = "SELECT 1 FROM user_achievement WHERE user_id = ? AND achievement_id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, achievementId);
            if (checkStmt.executeQuery().next()) {
                return "ALREADY_UNLOCKED";
            }
        }
        
        String sql = "INSERT INTO user_achievement (user_id, achievement_id, unlocked_at) VALUES (?, ?, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, achievementId);
            stmt.executeUpdate();
            return "SUCCESS";
        }
    }
    
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
    
    public List<Integer> getUserAchievements(int userId) throws SQLException {
        List<Integer> unlockedIds = new ArrayList<>();
        String sql = "SELECT ua.achievement_id FROM user_achievement ua " +
                     "INNER JOIN achievement a ON ua.achievement_id = a.achievement_id " +
                     "WHERE ua.user_id = ? AND a.game_id = ?";
        
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
    
    public void setupGame() throws SQLException {
        String checkSql = "SELECT game_id FROM videogame WHERE game_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
            stmt.setInt(1, GAME_ID);
            if (stmt.executeQuery().next()) {
                System.out.println("[Database] Game exists (ID: " + GAME_ID + ")");
                return;
            }
        }
        
        String insertSql = "INSERT INTO videogame (game_id, title, genre, release_date, developer, publisher) " +
                          "VALUES (?, 'Super Idol Clicker', 'Idle/Clicker', CURDATE(), 'Jorge Ferrando', 'Fruit Salad Ltd.')";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setInt(1, GAME_ID);
            stmt.executeUpdate();
        }
        setupAchievements();    
    }
    
    private void setupAchievements() throws SQLException {
        String sql = "INSERT IGNORE INTO achievement (game_id, name, description, points, is_secret) VALUES (?, ?, ?, ?, ?)";
        
        Object[][] achievements = {
            // Clicks
            {GAME_ID, "First Click", "Click for the first time", 10, false},
            {GAME_ID, "100 Clicks", "Your smile is sweeter than Super Idol's", 20, false},
            {GAME_ID, "1.000 Clicks", "Dedicated clicker", 50, false},
            {GAME_ID, "5.000 Clicks", "The water is always cold with Super Idol", 80, false},
            {GAME_ID, "15.000 Clicks", "You're a true Super Idol fan!", 140, false},
            {GAME_ID, "40.000 Clicks", "Super Idol is watching you", 220, false},
            {GAME_ID, "105.000ºC Clicks", "Your love for Super Idol is over 105 degrees!", 315, true},

            // Social Credits
            {GAME_ID, "+15 Social Credit", "Earn 100 credits", 10, false},
            {GAME_ID, "Good Citizen", "Earn 1.000 credits", 25, false},
            {GAME_ID, "Model Citizen", "Earn 10.000 credits", 40, false},
            {GAME_ID, "Super Idol's Favorite", "Earn 100.000 credits", 70, false},
            {GAME_ID, "Super Idol's Best Friend", "Earn 1.000.000 credits", 100, false},
            {GAME_ID, "Super Idol's Soulmate", "Earn 10.000.000 credits", 140, false},
            {GAME_ID, "Super Idol's Other Half", "Earn 100.000.000 credits", 170, false},
            {GAME_ID, "Super Idol's True Love", "Earn 1 billion of credits", 200, false},
            {GAME_ID, "Samba do Janeiro", "Earn 15B of credits", 240, false},
            {GAME_ID, "John Xina", "Earn 100B of credits", 290, false},
            {GAME_ID, "Supreme Idol", "Earn 1T of credits", 333, true},

            // Upgrades
            {GAME_ID, "First Upgrade", "Buy your first upgrade", 15, false},
            {GAME_ID, "Upgrade Collector", "Buy 10 upgrades", 40, false},
            {GAME_ID, "Upgrade Enthusiast", "Buy 50 upgrades", 80, false},
            {GAME_ID, "Upgrade Master", "Buy 200 upgrades", 130, false},
            {GAME_ID, "Upgrade Hoarder", "Buy 500 upgrades", 170, false},
            {GAME_ID, "Upgrade Addict", "Buy 1.000 upgrades", 230, false},
            {GAME_ID, "Upgrade Maniac", "Buy 5.000 upgrades", 270, false},
            {GAME_ID, "Upgrade Overlord", "Buy 20.000 upgrades", 300, false},
            {GAME_ID, "Upgrade God", "Buy 100.000 upgrades", 380, false},
            {GAME_ID, "Upgrade Legend", "Buy 500.000 upgrades", 450, true},
            
            // Playtime achievements
            {GAME_ID, "Loyal Fan", "Play for 1 hour total", 35, false},
            {GAME_ID, "Super Fan", "Play for 5 hours total", 80, false},
            {GAME_ID, "Idol Worshipper", "Play for 10 hours total", 150, false},
            {GAME_ID, "Idol Devotee", "Play for 30 hours total", 250, false},
            {GAME_ID, "Idol Addict", "Play for 80 hours total", 310, false},
            {GAME_ID, "Idol Maniac", "Play for 120 hours total", 390, false},
            {GAME_ID, "You gotta Love Super Idol", "Play for 250 hours total", 450, true}
        };
        
        // Insert achievements
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Object[] a : achievements) {
                stmt.setInt(1, (int) a[0]);
                stmt.setString(2, (String) a[1]);
                stmt.setString(3, (String) a[2]);
                stmt.setInt(4, (int) a[3]);
                stmt.setBoolean(5, (boolean) a[4]);
                stmt.executeUpdate();
            }
        }
    }   
}