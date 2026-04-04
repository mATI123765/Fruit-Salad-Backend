package com.fruitsalad.game;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the current state of Super Idol Clicker.
 * Manages social credits, upgrades, and all game statistics.
 */
public class GameState {
    
    /* PLAYER INFO */
    private int userId;
    private String username;
    private int sessionId;
    
    /* CORE GAME STATS */
    private double socialCredits;           // Current credits (can spend)
    private double totalCreditsEarned;      // Lifetime credits (for achievements)
    private long totalClicks;               // Total clicks ever
    private double creditsPerClick;         // How many credits per click
    private double creditsPerSecond;        // Passive income per second
    
    /* UPGRADES */
    private Map<String, Integer> upgrades;  // upgrade_name -> level
    
    // Upgrade definitions: {baseCost, costMultiplier, creditsPerClickBonus, creditsPerSecondBonus}
    private static final Map<String, double[]> UPGRADE_DEFS = new HashMap<>();
    private static final Map<String, String> UPGRADE_DESCRIPTIONS = new HashMap<>();
    
    static {
        // Click upgrades (increase credits per click)
        UPGRADE_DEFS.put("Microphone", new double[]{15, 1.4, 1, 0});
        UPGRADE_DESCRIPTIONS.put("Microphone", "Sing louder! +1 credit/click");
        
        UPGRADE_DEFS.put("Stage Lights", new double[]{50, 1.5, 3, 0});
        UPGRADE_DESCRIPTIONS.put("Stage Lights", "Shine brighter! +3 credits/click");
        
        UPGRADE_DEFS.put("Water Bottle 105°C", new double[]{200, 1.6, 10, 0});
        UPGRADE_DESCRIPTIONS.put("Water Bottle 105°C", "热爱105°C的你! +10 credits/click");
        
        UPGRADE_DEFS.put("Golden Smile", new double[]{1000, 1.7, 25, 0});
        UPGRADE_DESCRIPTIONS.put("Golden Smile", "Your smile is sweeter! +25 credits/click");
        
        // Passive upgrades (increase credits per second)
        UPGRADE_DEFS.put("Backup Dancers", new double[]{100, 1.5, 0, 1});
        UPGRADE_DESCRIPTIONS.put("Backup Dancers", "They work for you! +1 credit/sec");
        
        UPGRADE_DEFS.put("Fan Club", new double[]{500, 1.6, 0, 5});
        UPGRADE_DESCRIPTIONS.put("Fan Club", "Loyal supporters! +5 credits/sec");
        
        UPGRADE_DEFS.put("Music Video", new double[]{2500, 1.7, 0, 20});
        UPGRADE_DESCRIPTIONS.put("Music Video", "Goes viral! +20 credits/sec");
        
        UPGRADE_DEFS.put("Record Label", new double[]{10000, 1.8, 0, 100});
        UPGRADE_DESCRIPTIONS.put("Record Label", "Big money! +100 credits/sec");
        
        UPGRADE_DEFS.put("World Tour", new double[]{50000, 1.9, 0, 500});
        UPGRADE_DESCRIPTIONS.put("World Tour", "Global fame! +500 credits/sec");
    }
    
    /* CONSTRUCTOR */
    /**
     * Creates a new game state for a player
     */
    public GameState(int userId, String username) {
        this.userId = userId;
        this.username = username;
        this.sessionId = -1;
        
        // Initialize stats
        this.socialCredits = 0;
        this.totalCreditsEarned = 0;
        this.totalClicks = 0;
        this.creditsPerClick = 1;  // Start with 1 credit per click
        this.creditsPerSecond = 0; // No passive income initially
        
        // Initialize all upgrades to level 0
        this.upgrades = new HashMap<>();
        for (String upgradeName : UPGRADE_DEFS.keySet()) {
            upgrades.put(upgradeName, 0);
        }
    }
    
    /* CLICK LOGIC   */  
    /**
     * Processes a click and returns credits earned
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
     * Processes passive income (called every second by timer)
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
    
    /* UPGRADE LOGIC  */
    /**
     * Gets the cost of the next level of an upgrade
     * Formula: baseCost * (costMultiplier ^ currentLevel)
     */
    public double getUpgradeCost(String upgradeName) {
        if (!UPGRADE_DEFS.containsKey(upgradeName)) return -1;
        
        double[] def = UPGRADE_DEFS.get(upgradeName);
        int level = upgrades.getOrDefault(upgradeName, 0);
        
        // Cost increases exponentially with level
        return Math.floor(def[0] * Math.pow(def[1], level));
    }
    
    /**
     * Attempts to buy an upgrade
     * 
     * @param upgradeName The upgrade to buy
     * @return true if successful, false if can't afford
     */
    public boolean buyUpgrade(String upgradeName) {
        double cost = getUpgradeCost(upgradeName);
        
        // Check if upgrade exists and player can afford it
        if (cost < 0 || socialCredits < cost) {
            return false;
        }
        
        // Deduct cost
        socialCredits -= cost;
        
        // Increase upgrade level
        int newLevel = upgrades.getOrDefault(upgradeName, 0) + 1;
        upgrades.put(upgradeName, newLevel);
        
        // Apply the bonus
        double[] def = UPGRADE_DEFS.get(upgradeName);
        creditsPerClick += def[2];   // Add CPC bonus
        creditsPerSecond += def[3];  // Add CPS bonus
        
        return true;
    }
    
    /**
     * Checks if player can afford an upgrade
     */
    public boolean canAfford(String upgradeName) {
        double cost = getUpgradeCost(upgradeName);
        return cost > 0 && socialCredits >= cost;
    }
    
    /**
     * Gets all upgrade names in order
     */
    public static String[] getUpgradeNames() {
        // Return in specific order (click upgrades first, then passive)
        return new String[]{
            "Microphone", "Stage Lights", "Water Bottle 105°C", "Golden Smile",
            "Backup Dancers", "Fan Club", "Music Video", "Record Label", "World Tour"
        };
    }
    
    /**
     * Gets description for an upgrade
     */
    public static String getUpgradeDescription(String upgradeName) {
        return UPGRADE_DESCRIPTIONS.getOrDefault(upgradeName, "");
    }
    
    /**
     * Gets formatted info string for an upgrade (for display)
     */
    public String getUpgradeInfo(String upgradeName) {
        if (!UPGRADE_DEFS.containsKey(upgradeName)) return "";
        
        int level = upgrades.getOrDefault(upgradeName, 0);
        double cost = getUpgradeCost(upgradeName);
        String description = UPGRADE_DESCRIPTIONS.get(upgradeName);
        
        return String.format("Lv.%d | %s | Cost: %s", level, description, formatNumber(cost));
    }
    
    /* SAVE / LOAD FROM DATABASE */
    /**
     * Loads game state from database stats map
     */
    public void loadFromStats(Map<String, Double> stats) {
        this.socialCredits = stats.getOrDefault("social_credits", 0.0);
        this.totalCreditsEarned = stats.getOrDefault("total_credits_earned", 0.0);
        this.totalClicks = stats.getOrDefault("total_clicks", 0.0).longValue();
        this.creditsPerClick = stats.getOrDefault("credits_per_click", 1.0);
        this.creditsPerSecond = stats.getOrDefault("credits_per_second", 0.0);
        
        // Load upgrade levels
        for (String upgrade : UPGRADE_DEFS.keySet()) {
            String statName = "upgrade_" + upgrade.toLowerCase().replace(" ", "_").replace("°", "");
            int level = stats.getOrDefault(statName, 0.0).intValue();
            upgrades.put(upgrade, level);
        }
    }
    
    /**
     * Converts current state to stats map for saving to database
     */
    public Map<String, Double> toStatsMap() {
        Map<String, Double> stats = new HashMap<>();
        
        stats.put("social_credits", socialCredits);
        stats.put("total_credits_earned", totalCreditsEarned);
        stats.put("total_clicks", (double) totalClicks);
        stats.put("credits_per_click", creditsPerClick);
        stats.put("credits_per_second", creditsPerSecond);
        
        // Save upgrade levels
        for (Map.Entry<String, Integer> entry : upgrades.entrySet()) {
            String statName = "upgrade_" + entry.getKey().toLowerCase().replace(" ", "_").replace("°", "");
            stats.put(statName, entry.getValue().doubleValue());
        }
        
        return stats;
    }
    
    /* GETTERS */
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    
    public double getSocialCredits() { return socialCredits; }
    public double getTotalCreditsEarned() { return totalCreditsEarned; }
    public long getTotalClicks() { return totalClicks; }
    public double getCreditsPerClick() { return creditsPerClick; }
    public double getCreditsPerSecond() { return creditsPerSecond; }
    
    public int getUpgradeLevel(String upgradeName) {
        return upgrades.getOrDefault(upgradeName, 0);
    }
    
    public int getTotalUpgrades() {
        return upgrades.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /* UTILITY METHODS */
    /**
     * Formats a number for display
     * Examples: 1500 -> "1.5K", 2500000 -> "2.5M"
     */
    public static String formatNumber(double number) {
        if (number >= 1_000_000_000) {
            return String.format("%.2fB", number / 1_000_000_000);
        } else if (number >= 1_000_000) {
            return String.format("%.2fM", number / 1_000_000);
        } else if (number >= 1_000) {
            return String.format("%.2fK", number / 1_000);
        } else {
            return String.format("%.0f", number);
        }
    }
}