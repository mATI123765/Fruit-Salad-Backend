package com.fruitsalad.game;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * =============================================================================
 * GAME STATE
 * =============================================================================
 * Manages all game data including credits, upgrades, and statistics.
 * This is the core data model for Super Idol Clicker.
 * 
 * @author  Fruit Salad Ltd.
 * @version 1.0.0
 * =============================================================================
 */
public class GameState {
    
    // =========================================================================
    // PLAYER INFO
    // =========================================================================
    
    private final int userId;
    private final String username;
    private int sessionId;
    
    // =========================================================================
    // CORE STATISTICS
    // =========================================================================
    
    /** Current spendable social credits */
    private double socialCredits;
    
    /** Total credits ever earned (for achievements) */
    private double totalCreditsEarned;
    
    /** Total number of clicks */
    private long totalClicks;
    
    /** Credits earned per click */
    private double creditsPerClick;
    
    /** Passive credits earned per second */
    private double creditsPerSecond;
    
    // =========================================================================
    // UPGRADES
    // =========================================================================
    
    /** Map of upgrade name to current level */
    private final Map<String, Integer> upgradeLevels;
    
    // =========================================================================
    // UPGRADE DEFINITIONS
    // =========================================================================
    
    /**
     * Upgrade data class containing all upgrade properties.
     */
    public static class UpgradeData {
        public final String name;
        public final String description;
        public final String imagePath;
        public final double baseCost;
        public final double costMultiplier;
        public final double cpcBonus;      // Credits per click bonus
        public final double cpsBonus;      // Credits per second bonus
        
        public UpgradeData(String name, String description, String imagePath, 
                          double baseCost, double costMultiplier, 
                          double cpcBonus, double cpsBonus) {
            this.name = name;
            this.description = description;
            this.imagePath = imagePath;
            this.baseCost = baseCost;
            this.costMultiplier = costMultiplier;
            this.cpcBonus = cpcBonus;
            this.cpsBonus = cpsBonus;
        }
    }
    
    /** All available upgrades (ordered) */
    public static final LinkedHashMap<String, UpgradeData> UPGRADES = new LinkedHashMap<>();
    
    static {
        // Click upgrades (increase credits per click)
        UPGRADES.put("microphone", new UpgradeData(
            "Microphone", 
            "+1 credit per click",
            "/images/upgrades/1-upgrade.png",
            15, 1.15, 1, 0
        ));
        
        UPGRADES.put("stage_lights", new UpgradeData(
            "Stage Lights",
            "+3 credits per click",
            "/images/upgrades/2-upgrade.png",
            100, 1.15, 3, 0
        ));
        
        UPGRADES.put("water_bottle", new UpgradeData(
            "Water Bottle 105C",
            "+10 credits per click",
            "/images/upgrades/3-upgrade.png",
            500, 1.15, 10, 0
        ));
        
        UPGRADES.put("golden_smile", new UpgradeData(
            "Golden Smile",
            "+25 credits per click",
            "/images/upgrades/4-upgrade.png",
            2500, 1.15, 25, 0
        ));
        
        // Passive upgrades (increase credits per second)
        UPGRADES.put("backup_dancers", new UpgradeData(
            "Backup Dancers",
            "+1 credit per second",
            "/images/upgrades/5-upgrade.png",
            50, 1.15, 0, 1
        ));
        
        UPGRADES.put("fan_club", new UpgradeData(
            "Fan Club",
            "+5 credits per second",
            "/images/shop/1-item.png",
            300, 1.15, 0, 5
        ));
        
        UPGRADES.put("music_video", new UpgradeData(
            "Music Video",
            "+20 credits per second",
            "/images/shop/2-item.png",
            1500, 1.15, 0, 20
        ));
        
        UPGRADES.put("record_label", new UpgradeData(
            "Record Label",
            "+100 credits per second",
            "/images/shop/3-item.png",
            8000, 1.15, 0, 100
        ));
        
        UPGRADES.put("world_tour", new UpgradeData(
            "World Tour",
            "+500 credits per second",
            "/images/shop/4-item.png",
            50000, 1.15, 0, 500
        ));
    }
    
    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    
    /**
     * Creates a new game state for a player.
     * 
     * @param userId The player's user ID
     * @param username The player's username
     */
    public GameState(int userId, String username) {
        this.userId = userId;
        this.username = username;
        this.sessionId = -1;
        
        // Initialize statistics
        this.socialCredits = 0;
        this.totalCreditsEarned = 0;
        this.totalClicks = 0;
        this.creditsPerClick = 1;
        this.creditsPerSecond = 0;
        
        // Initialize all upgrades to level 0
        this.upgradeLevels = new LinkedHashMap<>();
        for (String key : UPGRADES.keySet()) {
            upgradeLevels.put(key, 0);
        }
    }
    
    // =========================================================================
    // CLICK LOGIC
    // =========================================================================
    
    /**
     * Processes a click and returns credits earned.
     * 
     * @return Amount of credits earned from this click
     */
    public double click() {
        double earned = creditsPerClick;
        socialCredits += earned;
        totalCreditsEarned += earned;
        totalClicks++;
        return earned;
    }
    
    /**
     * Processes passive income for one second.
     * 
     * @return Amount of credits earned passively
     */
    public double passiveIncome() {
        if (creditsPerSecond > 0) {
            socialCredits += creditsPerSecond;
            totalCreditsEarned += creditsPerSecond;
            return creditsPerSecond;
        }
        return 0;
    }
    
    // =========================================================================
    // UPGRADE LOGIC
    // =========================================================================
    
    /**
     * Gets the cost of the next level of an upgrade.
     * Formula: baseCost * (costMultiplier ^ currentLevel)
     * 
     * @param upgradeKey The upgrade identifier
     * @return The cost, or -1 if upgrade doesn't exist
     */
    public double getUpgradeCost(String upgradeKey) {
        UpgradeData data = UPGRADES.get(upgradeKey);
        if (data == null) return -1;
        
        int level = upgradeLevels.getOrDefault(upgradeKey, 0);
        return Math.floor(data.baseCost * Math.pow(data.costMultiplier, level));
    }
    
    /**
     * Attempts to purchase an upgrade.
     * 
     * @param upgradeKey The upgrade to buy
     * @return true if successful, false if cannot afford
     */
    public boolean buyUpgrade(String upgradeKey) {
        double cost = getUpgradeCost(upgradeKey);
        
        if (cost < 0 || socialCredits < cost) {
            return false;
        }
        
        UpgradeData data = UPGRADES.get(upgradeKey);
        
        // Deduct cost
        socialCredits -= cost;
        
        // Increase level
        int newLevel = upgradeLevels.getOrDefault(upgradeKey, 0) + 1;
        upgradeLevels.put(upgradeKey, newLevel);
        
        // Apply bonuses
        creditsPerClick += data.cpcBonus;
        creditsPerSecond += data.cpsBonus;
        
        return true;
    }
    
    /**
     * Checks if player can afford an upgrade.
     * 
     * @param upgradeKey The upgrade to check
     * @return true if player can afford it
     */
    public boolean canAfford(String upgradeKey) {
        double cost = getUpgradeCost(upgradeKey);
        return cost > 0 && socialCredits >= cost;
    }
    
    /**
     * Gets the current level of an upgrade.
     * 
     * @param upgradeKey The upgrade identifier
     * @return The current level
     */
    public int getUpgradeLevel(String upgradeKey) {
        return upgradeLevels.getOrDefault(upgradeKey, 0);
    }
    
    /**
     * Gets the total number of upgrades purchased.
     * 
     * @return Total upgrades count
     */
    public int getTotalUpgrades() {
        return upgradeLevels.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    // =========================================================================
    // SAVE / LOAD
    // =========================================================================
    
    /**
     * Loads game state from a stats map (from database).
     * 
     * @param stats Map of stat names to values
     */
    public void loadFromStats(Map<String, Double> stats) {
        this.socialCredits = stats.getOrDefault("social_credits", 0.0);
        this.totalCreditsEarned = stats.getOrDefault("total_credits_earned", 0.0);
        this.totalClicks = stats.getOrDefault("total_clicks", 0.0).longValue();
        this.creditsPerClick = stats.getOrDefault("credits_per_click", 1.0);
        this.creditsPerSecond = stats.getOrDefault("credits_per_second", 0.0);
        
        // Load upgrade levels
        for (String key : UPGRADES.keySet()) {
            String statName = "upgrade_" + key;
            int level = stats.getOrDefault(statName, 0.0).intValue();
            upgradeLevels.put(key, level);
        }
    }
    
    /**
     * Converts current state to a stats map for saving.
     * 
     * @return Map of stat names to values
     */
    public Map<String, Double> toStatsMap() {
        Map<String, Double> stats = new LinkedHashMap<>();
        
        stats.put("social_credits", socialCredits);
        stats.put("total_credits_earned", totalCreditsEarned);
        stats.put("total_clicks", (double) totalClicks);
        stats.put("credits_per_click", creditsPerClick);
        stats.put("credits_per_second", creditsPerSecond);
        
        // Save upgrade levels
        for (Map.Entry<String, Integer> entry : upgradeLevels.entrySet()) {
            stats.put("upgrade_" + entry.getKey(), entry.getValue().doubleValue());
        }
        
        return stats;
    }
    
    // =========================================================================
    // GETTERS
    // =========================================================================
    
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    
    public double getSocialCredits() { return socialCredits; }
    public double getTotalCreditsEarned() { return totalCreditsEarned; }
    public long getTotalClicks() { return totalClicks; }
    public double getCreditsPerClick() { return creditsPerClick; }
    public double getCreditsPerSecond() { return creditsPerSecond; }
    
    // =========================================================================
    // UTILITY
    // =========================================================================
    
    /**
     * Formats a number for display (K, M, B suffixes).
     * 
     * @param number The number to format
     * @return Formatted string
     */
    public static String formatNumber(double number) {
        if (number >= 1_000_000_000_000.0) {
            return String.format("%.2fT", number / 1_000_000_000_000.0);
        } else if (number >= 1_000_000_000) {
            return String.format("%.2fB", number / 1_000_000_000);
        } else if (number >= 1_000_000) {
            return String.format("%.2fM", number / 1_000_000);
        } else if (number >= 1_000) {
            return String.format("%.2fK", number / 1_000);
        } else if (number >= 100) {
            return String.format("%.0f", number);
        } else {
            return String.format("%.1f", number);
        }
    }
}
