package com.fruitsalad.game;

import com.fruitsalad.audio.SoundManager;
import com.fruitsalad.database.DatabaseManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * =============================================================================
 * GAME PANEL
 * =============================================================================
 * Main game interface inspired by Cookie Clicker.
 * Features a center clicker, left stats panel, and right upgrades panel.
 * 
 * @author  Fruit Salad Ltd.
 * @version 1.0.0
 * =============================================================================
 */
public class GamePanel extends JPanel implements ActionListener {
    
    // =========================================================================
    // COLOR SCHEME (Cookie Clicker inspired dark theme)
    // =========================================================================
    
    private static final Color BG_DARK = new Color(20, 20, 25);
    private static final Color BG_PANEL = new Color(30, 30, 38);
    private static final Color BG_PANEL_LIGHT = new Color(40, 40, 50);
    private static final Color ACCENT_RED = new Color(190, 50, 50);
    private static final Color ACCENT_GOLD = new Color(255, 200, 50);
    private static final Color ACCENT_GREEN = new Color(80, 180, 80);
    private static final Color TEXT_WHITE = new Color(240, 240, 240);
    private static final Color TEXT_GRAY = new Color(150, 150, 160);
    private static final Color TEXT_DARK = new Color(100, 100, 110);
    private static final Color BORDER_COLOR = new Color(60, 60, 70);
    
    // =========================================================================
    // GAME DATA
    // =========================================================================
    
    private final GameState gameState;
    private final DatabaseManager dbManager;
    private List<Integer> unlockedAchievements;
    
    // =========================================================================
    // IMAGES
    // =========================================================================
    
    private BufferedImage clickerImage;
    private BufferedImage backgroundImage;
    private final Map<String, BufferedImage> upgradeImages;
    
    // =========================================================================
    // ANIMATION
    // =========================================================================
    
    private final Timer gameTimer;
    private final Timer autoSaveTimer;
    private final List<FloatingText> floatingTexts;
    private final List<ClickParticle> clickParticles;
    private final Random random;
    
    private float clickerScale = 1.0f;
    private float targetScale = 1.0f;
    private int easterEggClicks = 0;
    
    // =========================================================================
    // UI COMPONENTS
    // =========================================================================
    
    private JLabel creditsLabel;
    private JLabel cpcLabel;
    private JLabel cpsLabel;
    private JPanel upgradesContainer;
    private JScrollPane upgradesScrollPane;
    
    // =========================================================================
    // CLICKER AREA
    // =========================================================================
    
    private Rectangle clickerBounds;
    private boolean clickerHovered = false;
    
    // =========================================================================
    // FLOATING TEXT CLASS
    // =========================================================================
    
    private static class FloatingText {
        double x, y;
        double vy;
        float alpha;
        String text;
        Color color;
        int fontSize;
        
        FloatingText(double x, double y, String text, Color color) {
            this.x = x;
            this.y = y;
            this.vy = -2.5;
            this.alpha = 1.0f;
            this.text = text;
            this.color = color;
            this.fontSize = 22;
        }
        
        void update() {
            y += vy;
            vy *= 0.98;
            alpha -= 0.025f;
        }
        
        boolean isDead() {
            return alpha <= 0;
        }
    }
    
    // =========================================================================
    // CLICK PARTICLE CLASS
    // =========================================================================
    
    private static class ClickParticle {
        double x, y;
        double vx, vy;
        float alpha;
        double size;
        Color color;
        
        ClickParticle(double x, double y, Random random) {
            this.x = x;
            this.y = y;
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = random.nextDouble() * 8 + 3;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
            this.alpha = 1.0f;
            this.size = random.nextDouble() * 8 + 4;
            
            // Gold/red particles
            if (random.nextBoolean()) {
                this.color = new Color(255, 200, 50);
            } else {
                this.color = new Color(255, 100, 100);
            }
        }
        
        void update() {
            x += vx;
            y += vy;
            vy += 0.3; // gravity
            vx *= 0.98;
            alpha -= 0.03f;
            size *= 0.97;
        }
        
        boolean isDead() {
            return alpha <= 0 || size < 1;
        }
    }
    
    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    
    /**
     * Creates the game panel.
     * 
     * @param gameState The game state object
     * @param dbManager The database manager
     */
    public GamePanel(GameState gameState, DatabaseManager dbManager) {
        this.gameState = gameState;
        this.dbManager = dbManager;
        this.floatingTexts = new ArrayList<>();
        this.clickParticles = new ArrayList<>();
        this.upgradeImages = new java.util.HashMap<>();
        this.random = new Random();
        this.unlockedAchievements = new ArrayList<>();
        
        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        
        // Load resources
        loadImages();
        
        // Create UI
        initializeUI();
        
        // Load game data
        loadGameData();
        
        // Start timers
        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();
        
        autoSaveTimer = new Timer(30000, e -> saveGameData()); // Save every 30s
        autoSaveTimer.start();
        
        // Mouse listeners for clicker
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (clickerBounds != null && clickerBounds.contains(e.getPoint())) {
                    onClickerPressed(e.getX(), e.getY());
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                targetScale = clickerHovered ? 1.05f : 1.0f;
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean wasHovered = clickerHovered;
                clickerHovered = clickerBounds != null && clickerBounds.contains(e.getPoint());
                
                if (clickerHovered != wasHovered) {
                    targetScale = clickerHovered ? 1.05f : 1.0f;
                    setCursor(clickerHovered ? 
                        new Cursor(Cursor.HAND_CURSOR) : 
                        new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        
        // Start game music
        SoundManager.getInstance().playGameMusic();
    }
    
    // =========================================================================
    // IMAGE LOADING
    // =========================================================================
    
    /**
     * Loads all game images.
     */
    private void loadImages() {
        try {
            // Main clicker image
            clickerImage = loadImage("/images/main-image-clicker.png");
            
            // Background
            backgroundImage = loadImage("/images/game_background.png");
            
            // Load upgrade images
            for (String key : GameState.UPGRADES.keySet()) {
                GameState.UpgradeData data = GameState.UPGRADES.get(key);
                BufferedImage img = loadImage(data.imagePath);
                if (img != null) {
                    upgradeImages.put(key, img);
                }
            }
            
            System.out.println("[GamePanel] Images loaded successfully");
            
        } catch (Exception e) {
            System.err.println("[GamePanel] Error loading images: " + e.getMessage());
        }
    }
    
    /**
     * Loads an image from resources.
     */
    private BufferedImage loadImage(String path) {
        try {
            var stream = getClass().getResourceAsStream(path);
            if (stream != null) {
                return ImageIO.read(stream);
            }
        } catch (IOException e) {
            System.err.println("[GamePanel] Failed to load: " + path);
        }
        return null;
    }
    
    // =========================================================================
    // UI INITIALIZATION
    // =========================================================================
    
    /**
     * Creates all UI components.
     */
    private void initializeUI() {
        // LEFT PANEL - Stats
        JPanel leftPanel = createStatsPanel();
        add(leftPanel, BorderLayout.WEST);
        
        // RIGHT PANEL - Upgrades
        JPanel rightPanel = createUpgradesPanel();
        add(rightPanel, BorderLayout.EAST);
        
        // CENTER PANEL - Clicker (painted in paintComponent)
        JPanel centerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // Don't call super - we paint everything custom
            }
        };
        centerPanel.setOpaque(false);
        add(centerPanel, BorderLayout.CENTER);
    }
    
    /**
     * Creates the stats panel (left side).
     */
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setPreferredSize(new Dimension(250, 0));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));
        
        // Title
        JLabel titleLabel = new JLabel("STATISTICS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(ACCENT_GOLD);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
        panel.add(titleLabel);
        
        // Social Credits
        panel.add(createStatBox("SOCIAL CREDITS", 
            creditsLabel = new JLabel("0")));
        creditsLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        creditsLabel.setForeground(ACCENT_GOLD);
        
        panel.add(Box.createVerticalStrut(15));
        
        // Credits per click
        panel.add(createStatBox("Per Click", 
            cpcLabel = new JLabel("1")));
        cpcLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        cpcLabel.setForeground(TEXT_WHITE);
        
        panel.add(Box.createVerticalStrut(10));
        
        // Credits per second
        panel.add(createStatBox("Per Second", 
            cpsLabel = new JLabel("0")));
        cpsLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        cpsLabel.setForeground(ACCENT_GREEN);
        
        panel.add(Box.createVerticalGlue());
        
        // Music toggle button
        JButton musicBtn = createToggleButton("Music: ON");
        musicBtn.addActionListener(e -> {
            boolean enabled = SoundManager.getInstance().toggleMusic();
            musicBtn.setText("Music: " + (enabled ? "ON" : "OFF"));
        });
        panel.add(musicBtn);
        
        panel.add(Box.createVerticalStrut(10));
        
        // Sound toggle button
        JButton soundBtn = createToggleButton("Sound: ON");
        soundBtn.addActionListener(e -> {
            boolean enabled = SoundManager.getInstance().toggleSound();
            soundBtn.setText("Sound: " + (enabled ? "ON" : "OFF"));
        });
        panel.add(soundBtn);
        
        panel.add(Box.createVerticalStrut(20));
        
        return panel;
    }
    
    /**
     * Creates a stat display box.
     */
    private JPanel createStatBox(String title, JLabel valueLabel) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(BG_PANEL_LIGHT);
        box.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.setMaximumSize(new Dimension(220, 80));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(10, 15, 10, 15)
        ));
        
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLbl.setForeground(TEXT_GRAY);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        box.add(titleLbl);
        box.add(Box.createVerticalStrut(5));
        box.add(valueLabel);
        
        return box;
    }
    
    /**
     * Creates a toggle button.
     */
    private JButton createToggleButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setBackground(BG_PANEL_LIGHT);
        button.setForeground(TEXT_GRAY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(200, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(60, 60, 70));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BG_PANEL_LIGHT);
            }
        });
        
        return button;
    }
    
    /**
     * Creates the upgrades panel (right side).
     */
    private JPanel createUpgradesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        panel.setPreferredSize(new Dimension(320, 0));
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_COLOR));
        
        // Title
        JLabel titleLabel = new JLabel("UPGRADES");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(ACCENT_GOLD);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(15, 0, 15, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Upgrades container
        upgradesContainer = new JPanel();
        upgradesContainer.setLayout(new BoxLayout(upgradesContainer, BoxLayout.Y_AXIS));
        upgradesContainer.setBackground(BG_PANEL);
        
        // Create upgrade rows
        for (String key : GameState.UPGRADES.keySet()) {
            upgradesContainer.add(createUpgradeRow(key));
            upgradesContainer.add(Box.createVerticalStrut(5));
        }
        
        upgradesScrollPane = new JScrollPane(upgradesContainer);
        upgradesScrollPane.setBackground(BG_PANEL);
        upgradesScrollPane.setBorder(null);
        upgradesScrollPane.getViewport().setBackground(BG_PANEL);
        upgradesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        upgradesScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(upgradesScrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates a single upgrade row.
     */
    private JPanel createUpgradeRow(String upgradeKey) {
        GameState.UpgradeData data = GameState.UPGRADES.get(upgradeKey);
        
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(BG_PANEL_LIGHT);
        row.setMaximumSize(new Dimension(300, 80));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
        row.setName(upgradeKey);
        
        // Left: Image
        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                BufferedImage img = upgradeImages.get(upgradeKey);
                if (img != null) {
                    g.drawImage(img, 0, 0, 55, 55, null);
                } else {
                    g.setColor(ACCENT_GOLD);
                    g.fillRect(5, 5, 45, 45);
                }
            }
        };
        imagePanel.setPreferredSize(new Dimension(55, 55));
        imagePanel.setOpaque(false);
        row.add(imagePanel, BorderLayout.WEST);
        
        // Center: Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(data.name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(TEXT_WHITE);
        
        JLabel descLabel = new JLabel(data.description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLabel.setForeground(TEXT_GRAY);
        
        JLabel levelLabel = new JLabel("Level: 0");
        levelLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        levelLabel.setForeground(TEXT_DARK);
        levelLabel.setName("level_" + upgradeKey);
        
        infoPanel.add(nameLabel);
        infoPanel.add(descLabel);
        infoPanel.add(levelLabel);
        row.add(infoPanel, BorderLayout.CENTER);
        
        // Right: Buy button
        JButton buyBtn = new JButton("BUY");
        buyBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        buyBtn.setBackground(ACCENT_RED);
        buyBtn.setForeground(Color.WHITE);
        buyBtn.setFocusPainted(false);
        buyBtn.setBorderPainted(false);
        buyBtn.setPreferredSize(new Dimension(70, 55));
        buyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        buyBtn.setName("buy_" + upgradeKey);
        
        buyBtn.addActionListener(e -> onBuyUpgrade(upgradeKey));
        
        buyBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (gameState.canAfford(upgradeKey)) {
                    buyBtn.setBackground(new Color(220, 70, 70));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                buyBtn.setBackground(gameState.canAfford(upgradeKey) ? ACCENT_RED : TEXT_DARK);
            }
        });
        
        row.add(buyBtn, BorderLayout.EAST);
        
        return row;
    }
    
    // =========================================================================
    // PAINTING
    // =========================================================================
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        // Draw center area background
        int centerX = 250; // After left panel
        int centerWidth = getWidth() - 250 - 320; // Between panels
        
        // Subtle gradient background
        GradientPaint bgGradient = new GradientPaint(
            centerX, 0, new Color(25, 25, 32),
            centerX + centerWidth, getHeight(), new Color(35, 35, 45)
        );
        g2d.setPaint(bgGradient);
        g2d.fillRect(centerX, 0, centerWidth, getHeight());
        
        // Draw clicker
        drawClicker(g2d, centerX, centerWidth);
        
        // Draw particles
        drawParticles(g2d);
        
        // Draw floating texts
        drawFloatingTexts(g2d);
        
        g2d.dispose();
    }
    
    /**
     * Draws the main clicker.
     */
    private void drawClicker(Graphics2D g2d, int centerX, int centerWidth) {
        int clickerSize = 220;
        int x = centerX + centerWidth / 2 - clickerSize / 2;
        int y = getHeight() / 2 - clickerSize / 2 - 30;
        
        // Store bounds for click detection
        clickerBounds = new Rectangle(x - 20, y - 20, clickerSize + 40, clickerSize + 40);
        
        // Glow effect when hovered
        if (clickerHovered) {
            int glowSize = (int) (clickerSize * 1.3f);
            int glowX = centerX + centerWidth / 2 - glowSize / 2;
            int glowY = getHeight() / 2 - glowSize / 2 - 30;
            
            RadialGradientPaint glow = new RadialGradientPaint(
                centerX + centerWidth / 2f, getHeight() / 2f - 30,
                glowSize / 2f,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 200, 50, 50), new Color(255, 200, 50, 0)}
            );
            g2d.setPaint(glow);
            g2d.fillOval(glowX, glowY, glowSize, glowSize);
        }
        
        // Draw clicker image with scale
        if (clickerImage != null) {
            int scaledSize = (int) (clickerSize * clickerScale);
            int scaledX = centerX + centerWidth / 2 - scaledSize / 2;
            int scaledY = getHeight() / 2 - scaledSize / 2 - 30;
            
            g2d.drawImage(clickerImage, scaledX, scaledY, scaledSize, scaledSize, null);
        } else {
            // Fallback circle
            g2d.setColor(ACCENT_GOLD);
            g2d.fillOval(x, y, clickerSize, clickerSize);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.drawString("CLICK!", x + clickerSize/2 - 40, y + clickerSize/2);
        }
        
        // Draw title below clicker
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 24));
        String title = "SUPER IDOL";
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.setColor(ACCENT_GOLD);
        g2d.drawString(title, centerX + centerWidth / 2 - titleWidth / 2, y + clickerSize + 40);
        
        // Subtitle
        g2d.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        String subtitle = "Click to earn Social Credits!";
        int subWidth = g2d.getFontMetrics().stringWidth(subtitle);
        g2d.setColor(TEXT_GRAY);
        g2d.drawString(subtitle, centerX + centerWidth / 2 - subWidth / 2, y + clickerSize + 65);
    }
    
    /**
     * Draws click particles.
     */
    private void drawParticles(Graphics2D g2d) {
        for (ClickParticle p : clickParticles) {
            int alpha = (int) (p.alpha * 255);
            if (alpha > 0) {
                g2d.setColor(new Color(
                    p.color.getRed(),
                    p.color.getGreen(),
                    p.color.getBlue(),
                    alpha
                ));
                int size = (int) p.size;
                g2d.fillOval((int) p.x - size/2, (int) p.y - size/2, size, size);
            }
        }
    }
    
    /**
     * Draws floating credit texts.
     */
    private void drawFloatingTexts(Graphics2D g2d) {
        for (FloatingText ft : floatingTexts) {
            int alpha = (int) (ft.alpha * 255);
            if (alpha > 0) {
                g2d.setFont(new Font("Segoe UI", Font.BOLD, ft.fontSize));
                g2d.setColor(new Color(
                    ft.color.getRed(),
                    ft.color.getGreen(),
                    ft.color.getBlue(),
                    alpha
                ));
                g2d.drawString(ft.text, (int) ft.x, (int) ft.y);
            }
        }
    }
    
    // =========================================================================
    // GAME LOGIC
    // =========================================================================
    
    /**
     * Called when the clicker is pressed.
     */
    private void onClickerPressed(int mouseX, int mouseY) {
        // Animate clicker
        targetScale = 0.9f;
        
        // Process click
        double earned = gameState.click();
        
        // Update UI
        refreshUI();
        
        // Add floating text
        floatingTexts.add(new FloatingText(
            mouseX - 30 + random.nextInt(60),
            mouseY - 20,
            "+" + GameState.formatNumber(earned),
            ACCENT_GOLD
        ));
        
        // Add particles
        for (int i = 0; i < 8; i++) {
            clickParticles.add(new ClickParticle(mouseX, mouseY, random));
        }
        
        // Easter egg: click 105 times in the corner
        if (mouseX < 300 && mouseY < 100) {
            easterEggClicks++;
            if (easterEggClicks == 105) {
                SoundManager.getInstance().playEasterEgg();
                floatingTexts.add(new FloatingText(
                    mouseX, mouseY - 50,
                    "PARADISE!",
                    new Color(255, 100, 255)
                ));
                easterEggClicks = 0;
            }
        }
        
        // Check achievements
        checkAchievements();
    }
    
    /**
     * Called when an upgrade is purchased.
     */
    private void onBuyUpgrade(String upgradeKey) {
        if (gameState.buyUpgrade(upgradeKey)) {
            // Success!
            SoundManager.getInstance().playUpgradeSound();
            refreshUI();
            updateUpgradeRows();
            
            // Record purchase in database
            try {
                GameState.UpgradeData data = GameState.UPGRADES.get(upgradeKey);
                dbManager.recordPurchase(gameState.getUserId(), data.name, 
                    gameState.getUpgradeCost(upgradeKey));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            checkAchievements();
        } else {
            // Cannot afford - play error sound or visual feedback
            Toolkit.getDefaultToolkit().beep();
        }
    }
    
    // =========================================================================
    // UI UPDATES
    // =========================================================================
    
    /**
     * Updates all UI elements with current game state.
     */
    private void refreshUI() {
        creditsLabel.setText(GameState.formatNumber(gameState.getSocialCredits()));
        cpcLabel.setText(GameState.formatNumber(gameState.getCreditsPerClick()));
        cpsLabel.setText(GameState.formatNumber(gameState.getCreditsPerSecond()));
    }
    
    /**
     * Updates upgrade row displays.
     */
    private void updateUpgradeRows() {
        for (Component comp : upgradesContainer.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel row = (JPanel) comp;
                String key = row.getName();
                if (key != null && GameState.UPGRADES.containsKey(key)) {
                    updateUpgradeRow(row, key);
                }
            }
        }
    }
    
    /**
     * Updates a single upgrade row.
     */
    private void updateUpgradeRow(JPanel row, String key) {
        for (Component c : row.getComponents()) {
            if (c instanceof JPanel) {
                for (Component inner : ((JPanel) c).getComponents()) {
                    if (inner instanceof JLabel && inner.getName() != null) {
                        if (inner.getName().equals("level_" + key)) {
                            ((JLabel) inner).setText("Level: " + gameState.getUpgradeLevel(key) + 
                                " | Cost: " + GameState.formatNumber(gameState.getUpgradeCost(key)));
                        }
                    }
                }
            }
            if (c instanceof JButton && c.getName() != null && c.getName().equals("buy_" + key)) {
                c.setBackground(gameState.canAfford(key) ? ACCENT_RED : TEXT_DARK);
            }
        }
    }
    
    // =========================================================================
    // ACHIEVEMENTS
    // =========================================================================
    
    /**
     * Checks and unlocks achievements.
     */
    private void checkAchievements() {
        try {
            // Click achievements
            checkAchievement("First Click", gameState.getTotalClicks() >= 1);
            checkAchievement("100 Clicks", gameState.getTotalClicks() >= 100);
            checkAchievement("1000 Clicks", gameState.getTotalClicks() >= 1000);
            checkAchievement("10000 Clicks", gameState.getTotalClicks() >= 10000);
            checkAchievement("50000 Clicks", gameState.getTotalClicks() >= 50000);
            checkAchievement("100000 Clicks", gameState.getTotalClicks() >= 100000);
            checkAchievement("1 Million Clicks", gameState.getTotalClicks() >= 1000000);
            
            // Credit achievements
            checkAchievement("+15 Social Credit", gameState.getTotalCreditsEarned() >= 100);
            checkAchievement("Good Citizen", gameState.getTotalCreditsEarned() >= 1000);
            checkAchievement("Model Citizen", gameState.getTotalCreditsEarned() >= 10000);
            checkAchievement("Super Idol's Favorite", gameState.getTotalCreditsEarned() >= 100000);
            checkAchievement("Supreme Leader", gameState.getTotalCreditsEarned() >= 1000000);
            
            // Upgrade achievements
            checkAchievement("First Upgrade", gameState.getTotalUpgrades() >= 1);
            checkAchievement("Upgrade Enthusiast", gameState.getTotalUpgrades() >= 10);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Checks and potentially unlocks an achievement.
     */
    private void checkAchievement(String name, boolean condition) throws SQLException {
        if (!condition) return;
        
        List<Map<String, Object>> achievements = dbManager.getGameAchievements();
        for (Map<String, Object> achievement : achievements) {
            if (achievement.get("name").equals(name)) {
                int id = (int) achievement.get("id");
                if (!unlockedAchievements.contains(id)) {
                    String result = dbManager.unlockAchievement(gameState.getUserId(), id);
                    if (result.startsWith("SUCCESS")) {
                        unlockedAchievements.add(id);
                        showAchievementNotification(name);
                    }
                }
                break;
            }
        }
    }
    
    /**
     * Shows an achievement unlock notification.
     */
    private void showAchievementNotification(String achievementName) {
        SoundManager.getInstance().playCreditSound();
        
        // Add big floating text
        int centerX = getWidth() / 2;
        FloatingText notification = new FloatingText(
            centerX - 100,
            getHeight() / 2 - 100,
            "Achievement: " + achievementName,
            ACCENT_GREEN
        );
        notification.fontSize = 18;
        notification.vy = -1;
        floatingTexts.add(notification);
    }
    
    // =========================================================================
    // DATA PERSISTENCE
    // =========================================================================
    
    /**
     * Loads game data from database.
     */
    private void loadGameData() {
        try {
            Map<String, Double> stats = dbManager.getAllStats(gameState.getUserId());
            if (!stats.isEmpty()) {
                gameState.loadFromStats(stats);
                System.out.println("[GamePanel] Game data loaded");
            }
            
            unlockedAchievements = dbManager.getUserAchievements(gameState.getUserId());
            
            int sessionId = dbManager.startGameSession(gameState.getUserId());
            gameState.setSessionId(sessionId);
            
            refreshUI();
            updateUpgradeRows();
            
        } catch (SQLException e) {
            System.err.println("[GamePanel] Error loading game data: " + e.getMessage());
        }
    }
    
    /**
     * Saves game data to database.
     */
    public void saveGameData() {
        try {
            Map<String, Double> stats = gameState.toStatsMap();
            for (Map.Entry<String, Double> entry : stats.entrySet()) {
                dbManager.recordStat(gameState.getUserId(), entry.getKey(), entry.getValue(), "set");
            }
            System.out.println("[GamePanel] Game saved");
        } catch (SQLException e) {
            System.err.println("[GamePanel] Error saving: " + e.getMessage());
        }
    }
    
    /**
     * Ends the current session.
     */
    public void endSession() {
        try {
            saveGameData();
            if (gameState.getSessionId() > 0) {
                dbManager.endGameSession(gameState.getSessionId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // =========================================================================
    // ANIMATION
    // =========================================================================
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Smooth clicker scale animation
        clickerScale += (targetScale - clickerScale) * 0.3f;
        
        // Update floating texts
        floatingTexts.removeIf(FloatingText::isDead);
        for (FloatingText ft : floatingTexts) {
            ft.update();
        }
        
        // Update particles
        clickParticles.removeIf(ClickParticle::isDead);
        for (ClickParticle p : clickParticles) {
            p.update();
        }
        
        // Passive income
        if (gameState.getCreditsPerSecond() > 0) {
            // Apply fraction of CPS per frame (60 FPS)
            double frameIncome = gameState.getCreditsPerSecond() / 60.0;
            gameState.passiveIncome();
            refreshUI();
        }
        
        // Update upgrade affordability
        updateUpgradeRows();
        
        repaint();
    }
    
    /**
     * Stops all timers.
     */
    public void stopTimers() {
        if (gameTimer != null) gameTimer.stop();
        if (autoSaveTimer != null) autoSaveTimer.stop();
    }
}
