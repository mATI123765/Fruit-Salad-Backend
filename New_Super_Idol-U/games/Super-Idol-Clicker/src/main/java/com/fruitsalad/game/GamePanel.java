package com.fruitsalad.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.fruitsalad.audio.SoundManager;
import com.fruitsalad.database.DatabaseManager;

/**
 * =============================================================================
 * GAME PANEL - Cookie Clicker Style
 * =============================================================================
 * Main game interface with three columns:
 * LEFT: Stats and info
 * CENTER: Big clicker with effects
 * RIGHT: Upgrades shop
 * 
 * @author  Fruit Salad Ltd.
 * @version 1.0.0
 * =============================================================================
 */
public class GamePanel extends JPanel implements ActionListener {
    
    /* COLORS (Dark Theme) */
    private static final Color BG_DARK = new Color(28, 28, 35);
    private static final Color BG_LEFT = new Color(35, 35, 45);
    private static final Color BG_CENTER = new Color(20, 25, 35);
    private static final Color BG_RIGHT = new Color(40, 35, 50);
    private static final Color GOLD = new Color(255, 210, 80);
    private static final Color RED = new Color(200, 60, 60);
    private static final Color GREEN = new Color(100, 200, 100);
    private static final Color TEXT_WHITE = new Color(240, 240, 245);
    private static final Color TEXT_GRAY = new Color(160, 160, 170);
    private static final Color TEXT_DARK = new Color(100, 100, 110);
    private static final Color UPGRADE_BG = new Color(55, 50, 65);
    private static final Color UPGRADE_HOVER = new Color(70, 65, 85);
    private static final Color UPGRADE_LOCKED = new Color(45, 45, 55);

    /* LAYOUT */    
    private static final int LEFT_WIDTH = 280;
    private static final int RIGHT_WIDTH = 340;
    
    /* GAME DATA */
    private final GameState gameState;
    private final DatabaseManager dbManager;
    private List<Integer> unlockedAchievements;
    
    /* IMAGES */
    private BufferedImage clickerImage;
    private BufferedImage backgroundImage;
    private final java.util.Map<String, BufferedImage> upgradeImages = new java.util.HashMap<>();
    
    /* ANIMATION */
    private final Timer gameTimer;
    private final Timer autoSaveTimer;
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    
    /* Clicks variables */
    private float clickerAngle = 0;
    private float clickerScale = 1.0f;
    private float targetScale = 1.0f;
    private float backgroundOffset = 0;
    private int comboClicks = 0;
    private long lastClickTime = 0;
    private long lastPassiveIncomeTime = 0;
    
    /* UI STATE */
    private Rectangle clickerBounds = new Rectangle();
    private boolean clickerHovered = false;
    private int hoveredUpgrade = -1;
    private int scrollOffset = 0;
    // Achievements
    private int achievementScrollOffset = 0;
    private int maxAchievementScroll = 0;
    private int hoveredAchievement = -1;
    private String hoveredAchievementDesc = "";
    private Rectangle achievementAreaBounds = new Rectangle();
    
    /* FLOATING TEXT */
    private static class FloatingText {
        float x, y, vy, alpha;
        String text;
        Color color;
        int size;
        
        FloatingText(float x, float y, String text, Color color, int size) {
            this.x = x;
            this.y = y;
            this.vy = -3f;
            this.alpha = 1f;
            this.text = text;
            this.color = color;
            this.size = size;
        }
        
        void update() {
            y += vy;
            vy *= 0.96f;
            alpha -= 0.02f;
        }
        
        boolean isDead() { return alpha <= 0; }
    }
    
    /* PARTICLE */
    private static class Particle {
        float x, y, vx, vy, alpha, size;
        Color color;
        
        Particle(float x, float y, Random r) {
            this.x = x;
            this.y = y;
            double angle = r.nextDouble() * Math.PI * 2;
            double speed = r.nextDouble() * 12 + 4;
            this.vx = (float) (Math.cos(angle) * speed);
            this.vy = (float) (Math.sin(angle) * speed);
            this.alpha = 1f;
            this.size = r.nextFloat() * 10 + 5;
            this.color = r.nextBoolean() ? GOLD : new Color(255, 150, 50);
        }
        
        void update() {
            x += vx;
            y += vy;
            vy += 0.4f;
            vx *= 0.98f;
            alpha -= 0.025f;
            size *= 0.97f;
        }
        
        boolean isDead() { return alpha <= 0 || size < 1; }
    }
    
    /* CONSTRUCTOR */
    public GamePanel(GameState gameState, DatabaseManager dbManager) {
        this.gameState = gameState;
        this.dbManager = dbManager;
        this.unlockedAchievements = new ArrayList<>();
        
        setBackground(BG_DARK);
        setFocusable(true);
        
        loadImages();
        loadGameData();
        
        // Mouse listeners
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                targetScale = clickerHovered ? 1.08f : 1.0f;
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e.getX(), e.getY());
            }
        });
        
        addMouseWheelListener(e -> {
            int mx = e.getX();
            int scroll = e.getWheelRotation() * 25;

            if (mx < LEFT_WIDTH) {
                // Scroll achievements (left panel)
                achievementScrollOffset += scroll;
                achievementScrollOffset = Math.max(0, Math.min(achievementScrollOffset, maxAchievementScroll));
            } else if (mx > getWidth() - RIGHT_WIDTH) {
                // Scroll upgrades (right panel)
                int totalUpgrades = GameState.UPGRADES.size();
                int upgradeHeight = 85;
                int visibleHeight = getHeight() - 70;
                int maxScroll = Math.max(0, (totalUpgrades * upgradeHeight) - visibleHeight + 50);
                
                scrollOffset += scroll;
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            }
            repaint();
        });
        
        // Initialize passive income timer
        lastPassiveIncomeTime = System.currentTimeMillis();

        // Game loop - 60 FPS
        gameTimer = new Timer(16, this);
        gameTimer.start();
        
        // Auto-save every 30 seconds
        autoSaveTimer = new Timer(30000, e -> saveGameData());
        autoSaveTimer.start();
        
        // Start game music
        SoundManager.getInstance().playGameMusic();
    }
    
    /* IMAGE LOADING */
    private void loadImages() {
        try {
            clickerImage = loadImage("/images/main-image-clicker.png");
            backgroundImage = loadImage("/images/game_background.png");
            
            // Load upgrade images
            for (String key : GameState.UPGRADES.keySet()) {
                GameState.UpgradeData data = GameState.UPGRADES.get(key);
                BufferedImage img = loadImage(data.imagePath);
                if (img != null) upgradeImages.put(key, img);
            }
            
            System.out.println("[GamePanel] Images loaded");
        } catch (Exception e) {
            System.err.println("[GamePanel] Error loading images: " + e.getMessage());
        }
    }
    
    private BufferedImage loadImage(String path) {
        try {
            var stream = getClass().getResourceAsStream(path);
            if (stream != null) return ImageIO.read(stream);
        } catch (IOException e) { }
        return null;
    }
    
    /* PAINTING */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        int w = getWidth();
        int h = getHeight();
        int centerWidth = w - LEFT_WIDTH - RIGHT_WIDTH;
        
        // Draw three panels
        drawLeftPanel(g2, 0, 0, LEFT_WIDTH, h);
        drawCenterPanel(g2, LEFT_WIDTH, 0, centerWidth, h);
        drawRightPanel(g2, LEFT_WIDTH + centerWidth, 0, RIGHT_WIDTH, h);
        
        // Draw particles
        drawParticles(g2);
        
        // Draw floating texts
        drawFloatingTexts(g2);
        
        // Draw achievement tooltip
        if (hoveredAchievement >= 0 && !hoveredAchievementDesc.isEmpty()) {
            drawAchievementTooltip(g2);
        }
        
        g2.dispose();
    }
    
    /* LEFT PANEL - STATS */
    private void drawLeftPanel(Graphics2D g2, int x, int y, int w, int h) {
        // Background
        g2.setColor(BG_LEFT);
        g2.fillRect(x, y, w, h);
        
        // Decorative top bar
        GradientPaint topGrad = new GradientPaint(x, y, GOLD, x + w, y, new Color(200, 150, 50));
        g2.setPaint(topGrad);
        g2.fillRect(x, y, w, 5);
        
        int py = 30;
        
        // Title
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.setColor(GOLD);
        drawCenteredString(g2, "SUPER IDOL", x, py, w);
        
        py += 25;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(TEXT_GRAY);
        drawCenteredString(g2, "CLICKER", x, py, w);
        
        // Main credits display
        py += 50;
        g2.setColor(new Color(50, 50, 60));
        g2.fillRoundRect(x + 15, py - 5, w - 30, 80, 15, 15);
        g2.setColor(new Color(70, 70, 80));
        g2.drawRoundRect(x + 15, py - 5, w - 30, 80, 15, 15);
        
        py += 20;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 32));
        g2.setColor(GOLD);
        drawCenteredString(g2, GameState.formatNumber(gameState.getSocialCredits()), x, py, w);
        
        py += 25;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(TEXT_GRAY);
        drawCenteredString(g2, "Social Credits", x, py, w);
        
        // Stats box
        py += 60;
        drawStatBox(g2, x + 15, py, w - 30, "Per Click", 
            GameState.formatNumber(gameState.getCreditsPerClick()), GOLD);
        
        py += 55;
        drawStatBox(g2, x + 15, py, w - 30, "Per Second", 
            GameState.formatNumber(gameState.getCreditsPerSecond()), GREEN);
        
        py += 55;
        drawStatBox(g2, x + 15, py, w - 30, "Total Clicks", 
            GameState.formatNumber(gameState.getTotalClicks()), TEXT_WHITE);
        
        py += 55;
        drawStatBox(g2, x + 15, py, w - 30, "Total Earned", 
            GameState.formatNumber(gameState.getTotalCreditsEarned()), GOLD);
        
        // Achievements section
        py += 60;
        g2.setColor(new Color(70, 70, 80));
        g2.drawLine(x + 20, py, x + w - 20, py);
        
        py += 20;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(GOLD);
        drawCenteredString(g2, "ACHIEVEMENTS", x, py, w);
        
        py += 10;
        
        // Achievements scrollable area
        int achievementStartY = py;
        int achievementAreaHeight = h - py - 130; // Space for buttons at bottom
        
        // Clip to achievements area
        Shape oldClip = g2.getClip();
        g2.setClip(x + 5, achievementStartY, w - 10, achievementAreaHeight);
        
        try {
            List<Map<String, Object>> achievements = dbManager.getGameAchievements();
            int achievementCount = achievements.size();
            int unlockedCount = 0;
            
            // Count unlocked
            for (Map<String, Object> achievement : achievements) {
                int id = (int) achievement.get("id");
                if (unlockedAchievements.contains(id)) unlockedCount++;
            }
            
            // Draw achievements with scroll offset
            int achY = achievementStartY + 10 - achievementScrollOffset;
            int achIndex = 0;
            
            // Store achievement area bounds for mouse detection
            achievementAreaBounds.setBounds(x + 5, achievementStartY, w - 10, achievementAreaHeight);
            
            for (Map<String, Object> achievement : achievements) {
                int id = (int) achievement.get("id");
                String name = (String) achievement.get("name");
                String description = (String) achievement.get("description");
                boolean isSecret = (boolean) achievement.get("isSecret");
                boolean isUnlocked = unlockedAchievements.contains(id);
                boolean isHovered = (hoveredAchievement == achIndex);
                
                // Only draw if visible
                if (achY > achievementStartY - 30 && achY < achievementStartY + achievementAreaHeight) {
                    // Achievement row - highlight if hovered
                    if (isHovered) {
                        g2.setColor(new Color(70, 70, 90));
                    } else if (isUnlocked) {
                        g2.setColor(new Color(45, 55, 45));
                    } else {
                        g2.setColor(new Color(45, 45, 55));
                    }
                    g2.fillRoundRect(x + 12, achY, w - 24, 26, 6, 6);
                    
                    // Border if hovered
                    if (isHovered) {
                        g2.setColor(GOLD);
                        g2.drawRoundRect(x + 12, achY, w - 24, 26, 6, 6);
                    }
                    
                    // Icon
                    if (isUnlocked) {
                        g2.setColor(GREEN);
                        g2.fillOval(x + 18, achY + 5, 16, 16);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                        g2.drawString("O", x + 23, achY + 17);
                    } else {
                        g2.setColor(new Color(80, 80, 90));
                        g2.fillOval(x + 18, achY + 5, 16, 16);
                        g2.setColor(new Color(60, 60, 70));
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                        g2.drawString("?", x + 24, achY + 17);
                    }
                    
                    // Name (show ??? if secret and not unlocked)
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    String displayName = (isSecret && !isUnlocked) ? "???" : name;
                    g2.setColor(isUnlocked ? TEXT_WHITE : TEXT_DARK);
                    
                    // Truncate long names
                    if (displayName.length() > 20) {
                        displayName = displayName.substring(0, 18) + "..";
                    }
                    g2.drawString(displayName, x + 40, achY + 17);
                    
                    // Store description for tooltip if hovered
                    if (isHovered && !isSecret || (isSecret && isUnlocked)) {
                        hoveredAchievementDesc = description;
                    } else if (isHovered && isSecret && !isUnlocked) {
                        hoveredAchievementDesc = "Secret achievement - Keep playing!";
                    }
                }
                achY += 30;
                achIndex++;
            }
            
            // Calculate max scroll
            int totalAchievementHeight = achievements.size() * 30;
            maxAchievementScroll = Math.max(0, totalAchievementHeight - achievementAreaHeight + 20);
            
            // Restore clip
            g2.setClip(oldClip);
            
            // Scroll indicators
            if (achievementScrollOffset > 0) {
                g2.setColor(new Color(255, 255, 255, 150));
                int[] xPoints = {x + w/2 - 8, x + w/2 + 8, x + w/2};
                int[] yPoints = {achievementStartY + 15, achievementStartY + 15, achievementStartY + 5};
                g2.fillPolygon(xPoints, yPoints, 3);
            }
            if (achievementScrollOffset < maxAchievementScroll) {
                g2.setColor(new Color(255, 255, 255, 150));
                int bottomY = achievementStartY + achievementAreaHeight;
                int[] xPoints = {x + w/2 - 8, x + w/2 + 8, x + w/2};
                int[] yPoints = {bottomY - 15, bottomY - 15, bottomY - 5};
                g2.fillPolygon(xPoints, yPoints, 3);
            }
            
            // Progress bar below achievements area
            int progressY = achievementStartY + achievementAreaHeight + 5;
            g2.setColor(new Color(45, 45, 55));
            g2.fillRoundRect(x + 15, progressY, w - 30, 22, 6, 6);
            
            if (achievementCount > 0) {
                int progressWidth = (int) ((w - 34) * ((double) unlockedCount / achievementCount));
                g2.setColor(GREEN);
                g2.fillRoundRect(x + 17, progressY + 3, Math.max(0, progressWidth), 16, 4, 4);
            }
            
            g2.setColor(TEXT_WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            String progressText = unlockedCount + " / " + achievementCount;
            drawCenteredString(g2, progressText, x, progressY + 16, w);
            
        } catch (SQLException e) {
            g2.setClip(oldClip);
            g2.setColor(TEXT_DARK);
            g2.drawString("Error loading", x + 20, achievementStartY + 20);
        }

        // Bottom controls
        py = h - 120;
        
        // Music toggle
        g2.setColor(new Color(50, 50, 60));
        g2.fillRoundRect(x + 15, py, w - 30, 35, 8, 8);
        g2.setColor(TEXT_GRAY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        String musicText = "Music: " + (SoundManager.getInstance().isMusicEnabled() ? "ON" : "OFF");
        drawCenteredString(g2, musicText, x, py + 23, w);
        
        py += 45;

        // Sound toggle
        g2.setColor(new Color(50, 50, 60));
        g2.fillRoundRect(x + 15, py, w - 30, 35, 8, 8);
        g2.setColor(TEXT_GRAY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        String soundText = "Sound: " + (SoundManager.getInstance().isSoundEnabled() ? "ON" : "OFF");
        drawCenteredString(g2, soundText, x, py + 23, w);
    }
    
    private void drawStatBox(Graphics2D g2, int x, int y, int w, String label, String value, Color valueColor) {
        g2.setColor(new Color(45, 45, 55));
        g2.fillRoundRect(x, y, w, 45, 10, 10);
        
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(TEXT_DARK);
        g2.drawString(label, x + 12, y + 16);
        
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.setColor(valueColor);
        g2.drawString(value, x + 12, y + 36);
    }
    
    /* CENTER PANEL - CLICKER */
    private void drawCenterPanel(Graphics2D g2, int x, int y, int w, int h) {
        // Draw background image
        if (backgroundImage != null) {
            // Scale image to fill the panel
            g2.drawImage(backgroundImage, x, y, w, h, null);
            
            // Dark overlay for better readability
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(x, y, w, h);
        } else {
            // Fallback gradient if no image
            float hue = (backgroundOffset % 360) / 360f;
            Color bgColor1 = Color.getHSBColor(hue * 0.1f, 0.3f, 0.12f);
            Color bgColor2 = Color.getHSBColor(hue * 0.1f + 0.02f, 0.2f, 0.08f);
            
            GradientPaint bgGrad = new GradientPaint(x, y, bgColor1, x + w, h, bgColor2);
            g2.setPaint(bgGrad);
            g2.fillRect(x, y, w, h);
        }
        
        // Clicker
        int centerX = x + w / 2;
        int centerY = h / 2 - 30;
        int clickerSize = 240;
        
        // Glow effect
        if (clickerHovered || clickerScale < 1.0f) {
            float glowIntensity = clickerHovered ? 0.4f : 0.2f;
            for (int i = 5; i > 0; i--) {
                int glowAlpha = (int) (glowIntensity * 40 * (6 - i));
                g2.setColor(new Color(255, 200, 50, glowAlpha));
                int size = clickerSize + i * 25;
                g2.fillOval(centerX - size / 2, centerY - size / 2, size, size);
            }
        }
        
        // Clicker bounds
        int scaledSize = (int) (clickerSize * clickerScale);
        int clickX = centerX - scaledSize / 2;
        int clickY = centerY - scaledSize / 2;
        clickerBounds.setBounds(clickX - 20, clickY - 20, scaledSize + 40, scaledSize + 40);
        
        // Draw clicker image
        if (clickerImage != null) {
            g2.drawImage(clickerImage, clickX, clickY, scaledSize, scaledSize, null);
        } else {
            // Fallback
            g2.setColor(GOLD);
            g2.fillOval(clickX, clickY, scaledSize, scaledSize);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            drawCenteredString(g2, "CLICK!", centerX - scaledSize/2, centerY, scaledSize);
        }
        
        // Combo indicator
        if (comboClicks > 5) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
            g2.setColor(new Color(255, 100, 100));
            drawCenteredString(g2, "COMBO x" + comboClicks, x, centerY - clickerSize/2 - 40, w);
        }
        
        // Title below clicker
        g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g2.setColor(GOLD);
        drawCenteredString(g2, "SUPER IDOL", x, centerY + clickerSize/2 + 50, w);
        
        g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        g2.setColor(TEXT_GRAY);
        drawCenteredString(g2, "The smile sweeter than honey", x, centerY + clickerSize/2 + 75, w);
        
        // Click to earn text
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(TEXT_DARK);
        drawCenteredString(g2, "Click to earn " + GameState.formatNumber(gameState.getCreditsPerClick()) + " credits!", 
            x, h - 40, w);
    }
    
    /* RIGHT PANEL - UPGRADES */    
    private void drawRightPanel(Graphics2D g2, int x, int y, int w, int h) {
        // Background
        g2.setColor(BG_RIGHT);
        g2.fillRect(x, y, w, h);
        
        // Top bar
        GradientPaint topGrad = new GradientPaint(x, y, RED, x + w, y, new Color(150, 50, 50));
        g2.setPaint(topGrad);
        g2.fillRect(x, y, w, 5);
        
        // Title
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.setColor(TEXT_WHITE);
        drawCenteredString(g2, "UPGRADES", x, 35, w);
        
        // Separator
        g2.setColor(new Color(80, 70, 90));
        g2.drawLine(x + 20, 50, x + w - 20, 50);
        
        // Upgrades list
        int py = 65 - scrollOffset;
        int index = 0;
        
        for (String key : GameState.UPGRADES.keySet()) {
            if (py > -100 && py < h) {
                drawUpgradeRow(g2, x + 10, py, w - 20, key, index);
            }
            py += 85;
            index++;
        }
        
        // Scroll indicator
        if (scrollOffset > 0) {
            g2.setColor(new Color(255, 255, 255, 100));
            int[] xPoints = {x + w/2 - 10, x + w/2 + 10, x + w/2};
            int[] yPoints = {65, 65, 55};
            g2.fillPolygon(xPoints, yPoints, 3);
        }
    }
    
    private void drawUpgradeRow(Graphics2D g2, int x, int y, int w, String key, int index) {
        GameState.UpgradeData data = GameState.UPGRADES.get(key);
        int level = gameState.getUpgradeLevel(key);
        double cost = gameState.getUpgradeCost(key);
        boolean canAfford = gameState.canAfford(key);
        boolean isHovered = hoveredUpgrade == index;
        
        // Background
        Color bgColor = canAfford ? (isHovered ? UPGRADE_HOVER : UPGRADE_BG) : UPGRADE_LOCKED;
        g2.setColor(bgColor);
        g2.fillRoundRect(x, y, w, 75, 12, 12);
        
        // Border on hover
        if (isHovered && canAfford) {
            g2.setColor(GOLD);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x, y, w, 75, 12, 12);
            g2.setStroke(new BasicStroke(1));
        }
        
        // Image
        BufferedImage img = upgradeImages.get(key);
        if (img != null) {
            g2.drawImage(img, x + 8, y + 8, 58, 58, null);
        } else {
            g2.setColor(GOLD);
            g2.fillRoundRect(x + 8, y + 8, 58, 58, 8, 8);
        }
        
        // Level badge
        if (level > 0) {
            g2.setColor(new Color(80, 150, 80));
            g2.fillRoundRect(x + 50, y + 50, 22, 18, 6, 6);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.drawString(String.valueOf(level), x + 56, y + 64);
        }
        
        // Name
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(canAfford ? TEXT_WHITE : TEXT_DARK);
        g2.drawString(data.name, x + 75, y + 22);
        
        // Description
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(TEXT_GRAY);
        g2.drawString(data.description, x + 75, y + 38);
        
        // Cost
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(canAfford ? GOLD : new Color(120, 100, 80));
        g2.drawString(GameState.formatNumber(cost) + " credits", x + 75, y + 60);
        
        // Buy hint
        if (isHovered && canAfford) {
            g2.setColor(GREEN);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.drawString("CLICK TO BUY", x + w - 85, y + 60);
        }
    }
    
    /* PARTICLES AND EFFECTS */
    private void drawParticles(Graphics2D g2) {
        for (Particle p : particles) {
            if (p.alpha > 0) {
                g2.setColor(new Color(
                    p.color.getRed(), p.color.getGreen(), p.color.getBlue(),
                    (int)(p.alpha * 255)
                ));
                int s = (int) p.size;
                g2.fillOval((int)p.x - s/2, (int)p.y - s/2, s, s);
            }
        }
    }
    
    private void drawFloatingTexts(Graphics2D g2) {
        for (FloatingText ft : floatingTexts) {
            if (ft.alpha > 0) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, ft.size));
                g2.setColor(new Color(
                    ft.color.getRed(), ft.color.getGreen(), ft.color.getBlue(),
                    (int)(ft.alpha * 255)
                ));
                g2.drawString(ft.text, ft.x, ft.y);
            }
        }
    }
    
    /* INPUT HANDLING */
    private void handleClick(int mx, int my) {
        // Check clicker
        if (clickerBounds.contains(mx, my)) {
            onClickerClick(mx, my);
            return;
        }
        
        // Check upgrades
        int upgradeX = getWidth() - RIGHT_WIDTH + 10;
        int py = 65 - scrollOffset;
        int index = 0;
        
        for (String key : GameState.UPGRADES.keySet()) {
            Rectangle rect = new Rectangle(upgradeX, py, RIGHT_WIDTH - 20, 75);
            if (rect.contains(mx, my)) {
                onUpgradeClick(key);
                return;
            }
            py += 85;
            index++;
        }
        
        // Check music/sound toggles
        if (mx < LEFT_WIDTH && mx > 15) {
            int h = getHeight();
            // Music button area
            if (my > h - 120 && my < h - 75) {
                SoundManager.getInstance().toggleMusic();
                repaint();
            }
            // Sound button area
            else if (my > h - 75 && my < h - 30) {
                SoundManager.getInstance().toggleSound();
                repaint();
            }
        }
    }
    
    private void handleMouseMove(int mx, int my) {
        // Check clicker hover
        boolean wasHovered = clickerHovered;
        clickerHovered = clickerBounds.contains(mx, my);
        if (clickerHovered != wasHovered) {
            targetScale = clickerHovered ? 1.08f : 1.0f;
            setCursor(clickerHovered ? 
                new Cursor(Cursor.HAND_CURSOR) : 
                new Cursor(Cursor.DEFAULT_CURSOR));
        }
        
        // Check achievement hover (left panel)
        int oldHoveredAchievement = hoveredAchievement;
        hoveredAchievement = -1;
        hoveredAchievementDesc = "";
        
        if (mx < LEFT_WIDTH && achievementAreaBounds.contains(mx, my)) {
            // Calculate which achievement is being hovered
            int relativeY = my - achievementAreaBounds.y + achievementScrollOffset - 10;
            int achIndex = relativeY / 30;
            
            try {
                List<Map<String, Object>> achievements = dbManager.getGameAchievements();
                if (achIndex >= 0 && achIndex < achievements.size()) {
                    hoveredAchievement = achIndex;
                }
            } catch (SQLException e) {
                // Ignore
            }
        }
        
        if (hoveredAchievement != oldHoveredAchievement) {
            repaint();
        }
        
        // Check upgrade hover
        int upgradeX = getWidth() - RIGHT_WIDTH + 10;
        int py = 65 - scrollOffset;
        int newHovered = -1;
        int index = 0;
        
        for (String key : GameState.UPGRADES.keySet()) {
            Rectangle rect = new Rectangle(upgradeX, py, RIGHT_WIDTH - 20, 75);
            if (rect.contains(mx, my)) {
                newHovered = index;
                break;
            }
            py += 85;
            index++;
        }
        
        if (newHovered != hoveredUpgrade) {
            hoveredUpgrade = newHovered;
            if (newHovered >= 0) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        }
    }
    
    private void onClickerClick(int mx, int my) {
        // Animate
        targetScale = 0.85f;
        
        // Process click
        double earned = gameState.click();
        
        // Combo system
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 300) {
            comboClicks++;
        } else {
            comboClicks = 1;
        }
        lastClickTime = now;
        
        // Bonus for combo
        if (comboClicks > 10) { earned *= 1.5; }
        
        // Floating text
        String text = "+" + GameState.formatNumber(earned);
        int offsetX = random.nextInt(80) - 40;
        floatingTexts.add(new FloatingText(mx + offsetX, my - 20, text, GOLD, 22));
        
        // Particles
        for (int i = 0; i < 12; i++) {
            particles.add(new Particle(mx, my, random));
        }
        
        // Check achievements
        checkAchievements();
    }
    
    private void onUpgradeClick(String key) {
        if (gameState.buyUpgrade(key)) {
            SoundManager.getInstance().playUpgradeSound();
            
            // Floating text
            floatingTexts.add(new FloatingText(
                getWidth() - RIGHT_WIDTH / 2, 100,
                "Upgrade purchased!", GREEN, 18
            ));

            // Skip recording purchase to avoid errors if table doesn't exist
            // The upgrade is already saved via game stats

            checkAchievements();
        } else {
            // Can't afford
            floatingTexts.add(new FloatingText(
                getWidth() - RIGHT_WIDTH / 2, 100,
                "Not enough credits!", RED, 16
            ));
        }
    }
    
    /* GAME LOOP */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Animate clicker scale
        clickerScale += (targetScale - clickerScale) * 0.25f;
        
        // Animate background
        backgroundOffset += 0.5f;
        
        // Update floating texts
        floatingTexts.removeIf(FloatingText::isDead);
        for (FloatingText ft : floatingTexts) ft.update();
        
        // Update particles
        particles.removeIf(Particle::isDead);
        for (Particle p : particles) p.update();
        
        // Decay combo
        if (System.currentTimeMillis() - lastClickTime > 500) {
            comboClicks = Math.max(0, comboClicks - 1);
        }
        
        // Passive income - apply CPS every second
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPassiveIncomeTime >= 1000) {
            if (gameState.getCreditsPerSecond() > 0) {
                double earned = gameState.passiveIncome();

                // Show floating text for passive income
                if (earned > 0) {
                    int centerX = LEFT_WIDTH + (getWidth() - LEFT_WIDTH - RIGHT_WIDTH) / 2;
                    floatingTexts.add(new FloatingText(
                        centerX - 30, getHeight() / 2 + 100,
                        "+" + GameState.formatNumber(earned) + "/s", GREEN, 16
                    ));
                }
                
                // Check achievements
                checkAchievements();
            }
            lastPassiveIncomeTime = currentTime;
        }
        repaint();
    }
    
    /* ACHIEVEMENTS */
    private void checkAchievements() {
        try {
            // Clicks
            checkAchievement("First Click", gameState.getTotalClicks() >= 1);
            checkAchievement("100 Clicks", gameState.getTotalClicks() >= 100);
            checkAchievement("1.000 Clicks", gameState.getTotalClicks() >= 1000);
            checkAchievement("5.000 Clicks", gameState.getTotalClicks() >= 5000);
            checkAchievement("15.000 Clicks", gameState.getTotalClicks() >= 15000);
            checkAchievement("40.000 Clicks", gameState.getTotalClicks() >= 40000);
            checkAchievement("105.000ºC Clicks", gameState.getTotalClicks() >= 105000);
            // Social Credits
            checkAchievement("+15 Social Credit", gameState.getTotalCreditsEarned() >= 100);
            checkAchievement("Good Citizen", gameState.getTotalCreditsEarned() >= 1000);
            checkAchievement("Model Citizen", gameState.getTotalCreditsEarned() >= 10000);
            checkAchievement("Super Idol's Favorite", gameState.getTotalCreditsEarned() >= 100000);
            checkAchievement("Super Idol's Best Friend", gameState.getTotalCreditsEarned() >= 1000000);
            checkAchievement("Super Idol's Soulmate", gameState.getTotalCreditsEarned() >= 10000000);
            checkAchievement("Super Idol's Other Half", gameState.getTotalCreditsEarned() >= 100000000);
            checkAchievement("Super Idol's True Love", gameState.getTotalCreditsEarned() >= 1000000000);
            checkAchievement("Samba do Janeiro", gameState.getTotalCreditsEarned() >= 15000000000L);
            checkAchievement("John Xina", gameState.getTotalCreditsEarned() >= 100000000000L);
            checkAchievement("Supreme Idol", gameState.getTotalCreditsEarned() >= 1000000000000L);
            // Upgrades
            checkAchievement("First Upgrade", gameState.getTotalUpgrades() >= 1);
            checkAchievement("Upgrade Collector", gameState.getTotalUpgrades() >= 10);
            checkAchievement("Upgrade Enthusiast", gameState.getTotalUpgrades() >= 50);
            checkAchievement("Upgrade Master", gameState.getTotalUpgrades() >= 200);
            checkAchievement("Upgrade Hoarder", gameState.getTotalUpgrades() >= 500);
            checkAchievement("Upgrade Addict", gameState.getTotalUpgrades() >= 1000);
            checkAchievement("Upgrade Maniac", gameState.getTotalUpgrades() >= 5000);
            checkAchievement("Upgrade Overlord", gameState.getTotalUpgrades() >= 20000);
            checkAchievement("Upgrade God", gameState.getTotalUpgrades() >= 100000);
            checkAchievement("Upgrade Legend", gameState.getTotalUpgrades() >= 500000);
            // Playtime achievements
            
        } catch (SQLException e) {
            System.err.println("[GamePanel] Achievement error");
        }
    }
    
    private void checkAchievement(String name, boolean condition) throws SQLException {
        if (!condition) return;
        
        List<Map<String, Object>> achievements = dbManager.getGameAchievements();
        for (Map<String, Object> a : achievements) {
            if (a.get("name").equals(name)) {
                int id = (int) a.get("id");
                if (!unlockedAchievements.contains(id)) {
                    String result = dbManager.unlockAchievement(gameState.getUserId(), id);
                    if (result.equals("SUCCESS")) {
                        unlockedAchievements.add(id);
                        SoundManager.getInstance().playCreditSound();
                        floatingTexts.add(new FloatingText(
                            getWidth() / 2 - 80, 150,
                            "Achievement: " + name, GREEN, 20
                        ));
                    }
                }
                break;
            }
        }
    }

    private void drawAchievementTooltip(Graphics2D g2) {
        // Get mouse position
        Point mouse = getMousePosition();
        if (mouse == null) return;
        
        int tooltipX = mouse.x + 15;
        int tooltipY = mouse.y + 15;
        
        // Measure text
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        
        // Word wrap for long descriptions
        String[] words = hoveredAchievementDesc.split(" ");
        java.util.List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        int maxWidth = 180;
        
        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            if (fm.stringWidth(testLine) > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        int lineHeight = fm.getHeight();
        int tooltipWidth = maxWidth + 20;
        int tooltipHeight = lines.size() * lineHeight + 16;
        
        // Adjust position if tooltip goes off screen
        if (tooltipX + tooltipWidth > getWidth()) {
            tooltipX = mouse.x - tooltipWidth - 10;
        }
        if (tooltipY + tooltipHeight > getHeight()) {
            tooltipY = mouse.y - tooltipHeight - 10;
        }
        
        // Draw tooltip background
        g2.setColor(new Color(30, 30, 40, 240));
        g2.fillRoundRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 8, 8);
        
        // Border
        g2.setColor(GOLD);
        g2.drawRoundRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 8, 8);
        
        // Draw text
        g2.setColor(TEXT_WHITE);
        int textY = tooltipY + 14;
        for (String line : lines) {
            g2.drawString(line, tooltipX + 10, textY);
            textY += lineHeight;
        }
    }
    
    /* DATA */
    private void loadGameData() {
        try {
            Map<String, Double> stats = dbManager.getAllStats(gameState.getUserId());
            if (!stats.isEmpty()) {
                gameState.loadFromStats(stats);
            }
            unlockedAchievements = dbManager.getUserAchievements(gameState.getUserId());
            int sessionId = dbManager.startGameSession(gameState.getUserId());
            gameState.setSessionId(sessionId);
        } catch (SQLException e) {
            System.err.println("[GamePanel] Error loading data");
        }
    }
    
    public void saveGameData() {
        try {
            Map<String, Double> stats = gameState.toStatsMap();
            for (Map.Entry<String, Double> entry : stats.entrySet()) {
                dbManager.recordStat(gameState.getUserId(), entry.getKey(), entry.getValue(), "set");
            }
            System.out.println("[GamePanel] Saved");
        } catch (SQLException e) {
            System.err.println("[GamePanel] Save error: " + e.getMessage());
        }
    }
    
    public void endSession() {
        try {
            saveGameData();
            if (gameState.getSessionId() > 0) {
                dbManager.endGameSession(gameState.getSessionId());
            }
        } catch (SQLException e) { }
    }
    
    public void stopTimers() {
        if (gameTimer != null) gameTimer.stop();
        if (autoSaveTimer != null) autoSaveTimer.stop();
    }
    
    /* UTILITY */    
    private void drawCenteredString(Graphics2D g2, String text, int x, int y, int width) {
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (width - fm.stringWidth(text)) / 2;
        g2.drawString(text, textX, y);
    }
}