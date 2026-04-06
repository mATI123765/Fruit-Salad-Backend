package com.fruitsalad.game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.fruitsalad.database.DatabaseManager;

/**
 * Main game panel for Super Idol Clicker.
 * Displays the clicker interface with Super Idol theme
 */
public class GamePanel extends JPanel {
    
    // Game state and database
    private GameState gameState;
    private DatabaseManager dbManager;
    
    // Images
    private BufferedImage backgroundImage;
    private BufferedImage clickerImage;
    private BufferedImage socialCreditPositive;
    private BufferedImage socialCreditNegative;
    
    // UI Components
    private JLabel creditsLabel;
    private JLabel cpcLabel;
    private JLabel cpsLabel;
    private JLabel clicksLabel;
    private JButton clickButton;
    private JPanel upgradesPanel;
    private JTextArea achievementsArea;
    private JLabel floatingLabel;
    
    // Timers
    private Timer passiveIncomeTimer;
    private Timer autoSaveTimer;
    private Timer floatingTextTimer;
    
    // Achievement tracking
    private List<Integer> unlockedAchievements;
    
    // Colors - Super Idol theme (Red & Gold Chinese style)
    private static final Color MAIN_RED = new Color(200, 30, 30);
    private static final Color DARK_RED = new Color(139, 0, 0);
    private static final Color GOLD = new Color(255, 215, 0);
    private static final Color CREAM = new Color(255, 253, 240);
    private static final Color PANEL_BG = new Color(40, 40, 50, 220);
    
    /**
     * Creates the game panel
     */
    public GamePanel(GameState gameState, DatabaseManager dbManager) {
        this.gameState = gameState;
        this.dbManager = dbManager;
        
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Load images
        loadImages();
        
        // Initialize UI components
        initializeUI();
        
        // Load game data from database
        loadGameData();
        
        // Start timers
        startTimers();
    }
    
    /**
     * Load all game images from resources
     */
    private void loadImages() {
        try {
            // Load main clicker image
            clickerImage = loadImage("/images/main-image-clicker.png");
            
            // Load background
            backgroundImage = loadImage("/images/game_background.png");
            
            // Load social credit effects
            socialCreditPositive = loadImage("/images/effects/social_credit_positive.png");
            socialCreditNegative = loadImage("/images/effects/social_credit_negative.png");
            
            System.out.println("All images loaded successfully");
            
        } catch (Exception e) {
            System.err.println("Error loading images: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Helper method to load an image from resources
     */
    private BufferedImage loadImage(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is != null) {
                return ImageIO.read(is);
            } else {
                System.err.println("Image not found: " + path);
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error loading image " + path + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Paint the background image
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Draw background image (scaled to fit)
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            
            // Add semi-transparent overlay for readability
            g.setColor(new Color(0, 0, 0, 100));
            g.fillRect(0, 0, getWidth(), getHeight());
        } else {
            // Fallback gradient background
            Graphics2D g2d = (Graphics2D) g;
            GradientPaint gradient = new GradientPaint(
                0, 0, DARK_RED,
                0, getHeight(), MAIN_RED
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeUI() {
        // ===== TOP PANEL - Stats =====
        JPanel topPanel = createStatsPanel();
        add(topPanel, BorderLayout.NORTH);
        
        // ===== CENTER - Click Button =====
        JPanel centerPanel = createClickerPanel();
        add(centerPanel, BorderLayout.CENTER);
        
        // ===== RIGHT - Upgrades =====
        JPanel rightPanel = createUpgradesPanel();
        add(rightPanel, BorderLayout.EAST);
        
        // ===== BOTTOM - Achievements =====
        JPanel bottomPanel = createAchievementsPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Creates the stats display panel (top)
     */
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 15, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Social Credits display
        creditsLabel = createStatLabel("0", 36);
        panel.add(createStatBox("🌟 SOCIAL CREDITS", creditsLabel));
        
        // Credits per click
        cpcLabel = createStatLabel("1", 28);
        panel.add(createStatBox("📈 PER CLICK", cpcLabel));
        
        // Credits per second
        cpsLabel = createStatLabel("0", 28);
        panel.add(createStatBox("⏱️ PER SECOND", cpsLabel));
        
        // Total clicks
        clicksLabel = createStatLabel("0", 28);
        panel.add(createStatBox("👆 TOTAL CLICKS", clicksLabel));
        
        return panel;
    }
    
    /**
     * Creates a stat display box
     */
    private JPanel createStatBox(String title, JLabel valueLabel) {
        JPanel box = new JPanel(new BorderLayout(5, 5));
        box.setBackground(PANEL_BG);
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD, 2),
            new EmptyBorder(10, 15, 10, 15)
        ));
        
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(GOLD);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        box.add(titleLabel, BorderLayout.NORTH);
        box.add(valueLabel, BorderLayout.CENTER);
        
        return box;
    }
    
    /**
     * Creates a stat value label
     */
    private JLabel createStatLabel(String text, int fontSize) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.BOLD, fontSize));
        return label;
    }
    
    /**
     * Creates the center panel with the clickable Super Idol
     */
    private JPanel createClickerPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        
        // Create click button with Super Idol image
        if (clickerImage != null) {
            // Scale image to button size
            Image scaledImage = clickerImage.getScaledInstance(300, 250, Image.SCALE_SMOOTH);
            clickButton = new JButton(new ImageIcon(scaledImage));
            clickButton.setPreferredSize(new Dimension(320, 270));
        } else {
            // Fallback if image not found
            clickButton = new JButton("🎤 CLICK ME 🎤");
            clickButton.setFont(new Font("Arial", Font.BOLD, 32));
            clickButton.setPreferredSize(new Dimension(300, 200));
        }
        
        // Style the button
        clickButton.setBackground(GOLD);
        clickButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MAIN_RED, 4),
            BorderFactory.createRaisedBevelBorder()
        ));
        clickButton.setFocusPainted(false);
        clickButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clickButton.setContentAreaFilled(false);
        clickButton.setOpaque(true);
        
        // Click action
        clickButton.addActionListener(e -> onSuperIdolClick());
        
        // Click animation (button press effect)
        clickButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                clickButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(MAIN_RED, 4),
                    BorderFactory.createLoweredBevelBorder()
                ));
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                clickButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(MAIN_RED, 4),
                    BorderFactory.createRaisedBevelBorder()
                ));
            }
        });
        
        // Add floating text label (for +credit animations)
        floatingLabel = new JLabel("");
        floatingLabel.setForeground(GOLD);
        floatingLabel.setFont(new Font("Arial", Font.BOLD, 24));
        floatingLabel.setVisible(false);
        
        // Layout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(clickButton, gbc);
        
        // Add title below button
        JLabel titleLabel = new JLabel("超级偶像的笑容都没你的甜");
        titleLabel.setForeground(GOLD);
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        gbc.gridy = 1;
        gbc.insets = new Insets(15, 0, 0, 0);
        panel.add(titleLabel, gbc);
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("热爱105°C的你 🔥");
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        gbc.gridy = 2;
        gbc.insets = new Insets(5, 0, 0, 0);
        panel.add(subtitleLabel, gbc);
        
        return panel;
    }
    
    /**
     * Creates the upgrades panel (right side)
     */
    private JPanel createUpgradesPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(PANEL_BG);
        mainPanel.setPreferredSize(new Dimension(320, 0));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD, 2),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        // Title
        JLabel title = new JLabel("⬆️ UPGRADES ⬆️", SwingConstants.CENTER);
        title.setForeground(GOLD);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        mainPanel.add(title, BorderLayout.NORTH);
        
        // Upgrades list
        upgradesPanel = new JPanel();
        upgradesPanel.setLayout(new BoxLayout(upgradesPanel, BoxLayout.Y_AXIS));
        upgradesPanel.setBackground(PANEL_BG);
        
        // Create upgrade rows
        for (String upgradeName : GameState.getUpgradeNames()) {
            JPanel upgradeRow = createUpgradeRow(upgradeName);
            upgradesPanel.add(upgradeRow);
            upgradesPanel.add(Box.createVerticalStrut(8));
        }
        
        JScrollPane scrollPane = new JScrollPane(upgradesPanel);
        scrollPane.setBackground(PANEL_BG);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(PANEL_BG);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * Creates a single upgrade row
     */
    private JPanel createUpgradeRow(String upgradeName) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(new Color(60, 60, 70));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 110), 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
        row.setMaximumSize(new Dimension(300, 70));
        
        // Left side: upgrade info
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(upgradeName);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 13));
        
        JLabel detailLabel = new JLabel(gameState.getUpgradeInfo(upgradeName));
        detailLabel.setForeground(Color.LIGHT_GRAY);
        detailLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        detailLabel.setName("detail_" + upgradeName);
        
        infoPanel.add(nameLabel);
        infoPanel.add(detailLabel);
        
        // Right side: buy button
        JButton buyButton = new JButton("BUY");
        buyButton.setBackground(MAIN_RED);
        buyButton.setForeground(Color.WHITE);
        buyButton.setFont(new Font("Arial", Font.BOLD, 12));
        buyButton.setFocusPainted(false);
        buyButton.setPreferredSize(new Dimension(70, 45));
        buyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        buyButton.addActionListener(e -> onBuyUpgrade(upgradeName));
        
        row.add(infoPanel, BorderLayout.CENTER);
        row.add(buyButton, BorderLayout.EAST);
        
        return row;
    }
    
    /**
     * Creates the achievements panel (bottom)
     */
    private JPanel createAchievementsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BG);
        panel.setPreferredSize(new Dimension(0, 120));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD, 2),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel title = new JLabel("🏆 ACHIEVEMENTS", SwingConstants.LEFT);
        title.setForeground(GOLD);
        title.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(title, BorderLayout.NORTH);
        
        achievementsArea = new JTextArea();
        achievementsArea.setBackground(new Color(30, 30, 40));
        achievementsArea.setForeground(Color.LIGHT_GRAY);
        achievementsArea.setEditable(false);
        achievementsArea.setFont(new Font("Arial", Font.PLAIN, 11));
        achievementsArea.setText("Loading achievements...");
        achievementsArea.setLineWrap(true);
        achievementsArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(achievementsArea);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /* GAME LOGIC */   
    /**
     * Called when player clicks Super Idol
     */
    private void onSuperIdolClick() {
        double earned = gameState.click();
        refreshUI();
        checkAchievements();
        
        // Show floating text animation
        showFloatingText("+" + GameState.formatNumber(earned));
        
        // Play click sound (if implemented)
        // SoundManager.playClick();
    }
    
    /**
     * Called when player tries to buy an upgrade
     */
    private void onBuyUpgrade(String upgradeName) {
        if (gameState.buyUpgrade(upgradeName)) {
            // Success!
            refreshUI();
            updateUpgradesPanel();
            
            // Record purchase in database
            try {
                double cost = gameState.getUpgradeCost(upgradeName);
                dbManager.recordPurchase(gameState.getUserId(), upgradeName, cost);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            checkAchievements();
            
        } else {
            // Can't afford - show negative feedback
            Toolkit.getDefaultToolkit().beep();
            showFloatingText("Not enough credits!");
        }
    }
    
    /**
     * Shows floating text animation (e.g., "+15 credits")
     */
    private void showFloatingText(String text) {
        // Create a temporary label for the floating text
        JLabel floatLabel = new JLabel(text);
        floatLabel.setForeground(GOLD);
        floatLabel.setFont(new Font("Arial", Font.BOLD, 22));
        
        // Get position relative to click button
        try {
            Point btnLoc = clickButton.getLocationOnScreen();
            Point panelLoc = this.getLocationOnScreen();
            int x = btnLoc.x - panelLoc.x + clickButton.getWidth() / 2 - 40;
            int y = btnLoc.y - panelLoc.y - 30;
            
            floatLabel.setBounds(x, y, 150, 30);
            this.add(floatLabel);
            this.setComponentZOrder(floatLabel, 0);
            this.repaint();
            
            // Animate upward and fade out
            Timer animTimer = new Timer(40, null);
            final int[] frame = {0};
            animTimer.addActionListener(e -> {
                frame[0]++;
                floatLabel.setLocation(floatLabel.getX(), floatLabel.getY() - 4);
                
                // Fade out effect
                float alpha = 1.0f - (frame[0] / 25.0f);
                if (alpha > 0) {
                    floatLabel.setForeground(new Color(255, 215, 0, (int)(alpha * 255)));
                }
                
                if (frame[0] > 20) {
                    animTimer.stop();
                    GamePanel.this.remove(floatLabel);
                    GamePanel.this.repaint();
                }
            });
            animTimer.start();
            
        } catch (Exception e) {
            // Component not visible yet, ignore
        }
    }
    
    // UI UPDATES
    /**
     * Updates all UI elements with current game state
     */
    private void refreshUI() {
        creditsLabel.setText(GameState.formatNumber(gameState.getSocialCredits()));
        cpcLabel.setText(GameState.formatNumber(gameState.getCreditsPerClick()));
        cpsLabel.setText(GameState.formatNumber(gameState.getCreditsPerSecond()));
        clicksLabel.setText(GameState.formatNumber(gameState.getTotalClicks()));
    }
    
    /**
     * Updates the upgrades panel with current levels and costs
     */
    private void updateUpgradesPanel() {
        for (Component comp : upgradesPanel.getComponents()) {
            if (comp instanceof JPanel) {
                updateUpgradeRowDetails((JPanel) comp);
            }
        }
    }
    
    /**
     * Updates a single upgrade row's detail label
     */
    private void updateUpgradeRowDetails(JPanel row) {
        for (Component comp : row.getComponents()) {
            if (comp instanceof JPanel) {
                for (Component innerComp : ((JPanel) comp).getComponents()) {
                    if (innerComp instanceof JLabel) {
                        JLabel label = (JLabel) innerComp;
                        if (label.getName() != null && label.getName().startsWith("detail_")) {
                            String upgradeName = label.getName().replace("detail_", "");
                            label.setText(gameState.getUpgradeInfo(upgradeName));
                        }
                    }
                }
            }
        }
    }
    
    /* ACHIEVEMENTS */  
    /**
     * Checks and unlocks achievements based on current game state
     */
    private void checkAchievements() {
        try {
            // Click achievements
            if (gameState.getTotalClicks() >= 1) tryUnlockAchievement("First Click");
            if (gameState.getTotalClicks() >= 100) tryUnlockAchievement("100 Clicks");
            if (gameState.getTotalClicks() >= 1000) tryUnlockAchievement("1000 Clicks");
            if (gameState.getTotalClicks() >= 10000) tryUnlockAchievement("10000 Clicks");
            if (gameState.getTotalClicks() >= 50000) tryUnlockAchievement("50000 Clicks");
            if (gameState.getTotalClicks() >= 100000) tryUnlockAchievement("100000 Clicks");
            if (gameState.getTotalClicks() >= 1000000) tryUnlockAchievement("1 Million Clicks");
            
            // Credit achievements
            if (gameState.getTotalCreditsEarned() >= 100) tryUnlockAchievement("+15 Social Credit");
            if (gameState.getTotalCreditsEarned() >= 1000) tryUnlockAchievement("Good Citizen");
            if (gameState.getTotalCreditsEarned() >= 10000) tryUnlockAchievement("Model Citizen");
            if (gameState.getTotalCreditsEarned() >= 100000) tryUnlockAchievement("Super Idol's Favorite");
            if (gameState.getTotalCreditsEarned() >= 1000000) tryUnlockAchievement("Supreme Leader");
            
            // Upgrade achievements
            if (gameState.getTotalUpgrades() >= 1) tryUnlockAchievement("First Upgrade");
            if (gameState.getTotalUpgrades() >= 10) tryUnlockAchievement("Upgrade Enthusiast");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Attempts to unlock an achievement by name
     */
    private void tryUnlockAchievement(String achievementName) throws SQLException {
        List<Map<String, Object>> achievements = dbManager.getGameAchievements();
        
        for (Map<String, Object> achievement : achievements) {
            if (achievement.get("name").equals(achievementName)) {
                int achievementId = (int) achievement.get("id");
                
                // Check if not already unlocked
                if (!unlockedAchievements.contains(achievementId)) {
                    String result = dbManager.unlockAchievement(gameState.getUserId(), achievementId);
                    
                    if (result.startsWith("SUCCESS")) {
                        unlockedAchievements.add(achievementId);
                        showAchievementPopup(achievementName);
                        updateAchievementsDisplay();
                    }
                }
                break;
            }
        }
    }
    
    /**
     * Shows achievement unlock popup
     */
    private void showAchievementPopup(String achievementName) {
        // Create custom dialog with Super Idol theme
        JOptionPane.showMessageDialog(
            this,
            """
            \ud83c\udfc6 +15 SOCIAL CREDIT! \ud83c\udfc6
            
            Achievement Unlocked:
            """ + achievementName + "\n\n" +
            "超级偶像为你骄傲!",
            "Achievement Unlocked!",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Updates the achievements display area
     */
    private void updateAchievementsDisplay() {
        try {
            List<Map<String, Object>> allAchievements = dbManager.getGameAchievements();
            StringBuilder sb = new StringBuilder();
            
            int unlockedCount = 0;
            for (Map<String, Object> achievement : allAchievements) {
                int id = (int) achievement.get("id");
                String name = (String) achievement.get("name");
                boolean isSecret = (boolean) achievement.get("isSecret");
                boolean unlocked = unlockedAchievements.contains(id);
                
                if (unlocked) {
                    sb.append("✅ ").append(name).append("  ");
                    unlockedCount++;
                } else if (isSecret) {
                    sb.append("🔒 ???  ");
                } else {
                    sb.append("⬜ ").append(name).append("  ");
                }
            }
            
            double percentage = dbManager.getAchievementPercentage(gameState.getUserId());
            sb.append("\n\n📊 Completion: ").append(String.format("%.1f%%", percentage));
            sb.append(" (").append(unlockedCount).append("/").append(allAchievements.size()).append(")");
            
            achievementsArea.setText(sb.toString());
            
        } catch (SQLException e) {
            e.printStackTrace();
            achievementsArea.setText("Error loading achievements");
        }
    }
    
    /* DATA PERSISTENCE */    
    /**
     * Loads game data from database
     */
    private void loadGameData() {
        try {
            // Load stats
            Map<String, Double> stats = dbManager.getAllStats(gameState.getUserId());
            if (!stats.isEmpty()) {
                gameState.loadFromStats(stats);
                System.out.println("Game data loaded from database");
            }
            
            // Load unlocked achievements
            unlockedAchievements = dbManager.getUserAchievements(gameState.getUserId());
            
            // Start new session
            int sessionId = dbManager.startGameSession(gameState.getUserId());
            gameState.setSessionId(sessionId);
            
            // Update UI
            refreshUI();
            updateUpgradesPanel();
            updateAchievementsDisplay();
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading game data: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Saves game data to database
     */
    public void saveGameData() {
        try {
            Map<String, Double> stats = gameState.toStatsMap();
            for (Map.Entry<String, Double> entry : stats.entrySet()) {
                dbManager.recordStat(gameState.getUserId(), entry.getKey(), entry.getValue(), "set");
            }
            System.out.println("Game saved");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Ends the current game session
     */
    public void endSession() {
        try {
            saveGameData();
            if (gameState.getSessionId() > 0) {
                int duration = dbManager.endGameSession(gameState.getSessionId());
                System.out.println("Session ended. Duration: " + duration + " minutes");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /* TIMERS */
    /**
     * Starts game timers (passive income, auto-save)
     */
    private void startTimers() {
        // Passive income timer - every second
        passiveIncomeTimer = new Timer(1000, e -> {
            if (gameState.getCreditsPerSecond() > 0) {
                gameState.passiveIncome();
                refreshUI();
            }
        });
        passiveIncomeTimer.start();
        
        // Auto-save timer - every 30 seconds
        autoSaveTimer = new Timer(30000, e -> saveGameData());
        autoSaveTimer.start();
    }
    
    /**
     * Stops all timers
     */
    public void stopTimers() {
        if (passiveIncomeTimer != null) passiveIncomeTimer.stop();
        if (autoSaveTimer != null) autoSaveTimer.stop();
    }
}