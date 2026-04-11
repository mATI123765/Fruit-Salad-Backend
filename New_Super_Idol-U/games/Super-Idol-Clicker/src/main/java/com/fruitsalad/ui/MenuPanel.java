package com.fruitsalad.ui;

import com.fruitsalad.audio.SoundManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * =============================================================================
 * MENU PANEL
 * =============================================================================
 * Animated main menu with particle effects and login screen.
 * Features a grey animated background similar to Space Invaders menu.
 * 
 * @author  Fruit Salad Ltd.
 * @version 1.0.0
 * =============================================================================
 */
public class MenuPanel extends JPanel implements ActionListener {
    
    // =========================================================================
    // CONSTANTS
    // =========================================================================
    
    private static final Color BACKGROUND_DARK = new Color(25, 25, 30);
    private static final Color BACKGROUND_LIGHT = new Color(45, 45, 55);
    private static final Color ACCENT_RED = new Color(200, 50, 50);
    private static final Color ACCENT_GOLD = new Color(255, 200, 50);
    private static final Color TEXT_WHITE = new Color(240, 240, 240);
    private static final Color TEXT_GRAY = new Color(150, 150, 160);
    private static final Color PANEL_BG = new Color(35, 35, 45, 230);
    private static final Color INPUT_BG = new Color(55, 55, 65);
    private static final Color BUTTON_HOVER = new Color(220, 70, 70);
    
    // =========================================================================
    // ANIMATION
    // =========================================================================
    
    private final Timer animationTimer;
    private final List<Particle> particles;
    private final List<FloatingIcon> floatingIcons;
    private final Random random;
    private float titlePulse = 0;
    
    // =========================================================================
    // UI COMPONENTS
    // =========================================================================
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    private JButton[] quickLoginButtons;
    private BufferedImage superIdolImage;
    
    // =========================================================================
    // CALLBACK
    // =========================================================================
    
    private LoginCallback loginCallback;
    
    /**
     * Callback interface for login events.
     */
    public interface LoginCallback {
        void onLogin(String username, String passwordHash);
    }
    
    // =========================================================================
    // PARTICLE CLASS
    // =========================================================================
    
    private static class Particle {
        double x, y;
        double vx, vy;
        double size;
        double alpha;
        Color color;
        
        Particle(double x, double y, Random random) {
            this.x = x;
            this.y = y;
            this.vx = (random.nextDouble() - 0.5) * 0.5;
            this.vy = -random.nextDouble() * 0.5 - 0.2;
            this.size = random.nextDouble() * 3 + 1;
            this.alpha = random.nextDouble() * 0.5 + 0.2;
            
            // Random warm color (red/orange/gold)
            int r = 200 + random.nextInt(55);
            int g = random.nextInt(150);
            int b = random.nextInt(50);
            this.color = new Color(r, g, b);
        }
        
        void update() {
            x += vx;
            y += vy;
            alpha -= 0.002;
            size -= 0.01;
        }
        
        boolean isDead() {
            return alpha <= 0 || size <= 0;
        }
    }
    
    // =========================================================================
    // FLOATING ICON CLASS
    // =========================================================================
    
    private static class FloatingIcon {
        double x, y;
        double angle;
        double speed;
        double rotationSpeed;
        double size;
        String symbol;
        
        FloatingIcon(double x, double y, Random random) {
            this.x = x;
            this.y = y;
            this.angle = random.nextDouble() * Math.PI * 2;
            this.speed = random.nextDouble() * 0.3 + 0.1;
            this.rotationSpeed = (random.nextDouble() - 0.5) * 0.02;
            this.size = random.nextDouble() * 20 + 15;
            
            String[] symbols = {"*", "+", "o", "~"};
            this.symbol = symbols[random.nextInt(symbols.length)];
        }
        
        void update(int width, int height) {
            y += speed;
            angle += rotationSpeed;
            
            if (y > height + 50) {
                y = -50;
                x = new Random().nextDouble() * width;
            }
        }
    }
    
    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    
    /**
     * Creates the menu panel with animated background.
     */
    public MenuPanel() {
        setLayout(null);
        setBackground(BACKGROUND_DARK);
        
        this.random = new Random();
        this.particles = new ArrayList<>();
        this.floatingIcons = new ArrayList<>();
        
        // Load Super Idol image
        loadImages();
        
        // Initialize particles
        initializeParticles();
        
        // Create UI components
        initializeUI();
        
        // Start animation timer (60 FPS)
        animationTimer = new Timer(16, this);
        animationTimer.start();
        
        // Start menu music
        SoundManager.getInstance().playMenuMusic();
        
        // Handle resize
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutComponents();
            }
        });
    }
    
    // =========================================================================
    // INITIALIZATION
    // =========================================================================
    
    /**
     * Loads images for the menu.
     */
    private void loadImages() {
        try {
            superIdolImage = ImageIO.read(
                getClass().getResourceAsStream("/images/main-image-clicker.png")
            );
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("[MenuPanel] Could not load Super Idol image");
        }
    }
    
    /**
     * Initializes floating particles and icons.
     */
    private void initializeParticles() {
        // Create floating icons
        for (int i = 0; i < 15; i++) {
            floatingIcons.add(new FloatingIcon(
                random.nextDouble() * 1200,
                random.nextDouble() * 800,
                random
            ));
        }
    }
    
    /**
     * Creates all UI components.
     */
    private void initializeUI() {
        // Username field
        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        usernameField.setBackground(INPUT_BG);
        usernameField.setForeground(TEXT_WHITE);
        usernameField.setCaretColor(TEXT_WHITE);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 80), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        add(usernameField);
        
        // Password field
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        passwordField.setBackground(INPUT_BG);
        passwordField.setForeground(TEXT_WHITE);
        passwordField.setCaretColor(TEXT_WHITE);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 80), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        add(passwordField);
        
        // Login button
        loginButton = createStyledButton("START GAME");
        loginButton.addActionListener(e -> attemptLogin());
        add(loginButton);
        
        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(ACCENT_RED);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(statusLabel);
        
        // Quick login buttons
        String[] testUsers = {"Jorgito", "Joel", "Iker", "Zakaria"};
        quickLoginButtons = new JButton[testUsers.length];
        
        for (int i = 0; i < testUsers.length; i++) {
            final String user = testUsers[i];
            quickLoginButtons[i] = createQuickButton(user);
            quickLoginButtons[i].addActionListener(e -> {
                usernameField.setText(user);
                attemptLogin();
            });
            add(quickLoginButtons[i]);
        }
        
        // Enter key triggers login
        usernameField.addActionListener(e -> passwordField.requestFocus());
        passwordField.addActionListener(e -> attemptLogin());
    }
    
    /**
     * Creates a styled main button.
     */
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 18));
        button.setBackground(ACCENT_RED);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(BUTTON_HOVER);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(ACCENT_RED);
            }
        });
        
        return button;
    }
    
    /**
     * Creates a quick login button.
     */
    private JButton createQuickButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setBackground(new Color(60, 60, 70));
        button.setForeground(TEXT_GRAY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(80, 80, 90));
                button.setForeground(TEXT_WHITE);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(60, 60, 70));
                button.setForeground(TEXT_GRAY);
            }
        });
        
        return button;
    }
    
    /**
     * Layouts components based on panel size.
     */
    private void layoutComponents() {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        int fieldWidth = 280;
        int fieldHeight = 45;
        int buttonWidth = 280;
        int buttonHeight = 50;
        
        int startY = centerY - 20;
        
        // Username field
        usernameField.setBounds(centerX - fieldWidth/2, startY, fieldWidth, fieldHeight);
        
        // Password field
        passwordField.setBounds(centerX - fieldWidth/2, startY + 60, fieldWidth, fieldHeight);
        
        // Login button
        loginButton.setBounds(centerX - buttonWidth/2, startY + 135, buttonWidth, buttonHeight);
        
        // Status label
        statusLabel.setBounds(centerX - 200, startY + 195, 400, 25);
        
        // Quick login buttons
        int quickBtnWidth = 65;
        int quickBtnHeight = 30;
        int totalQuickWidth = quickBtnWidth * 4 + 15;
        int quickStartX = centerX - totalQuickWidth / 2;
        
        for (int i = 0; i < quickLoginButtons.length; i++) {
            quickLoginButtons[i].setBounds(
                quickStartX + i * (quickBtnWidth + 5),
                startY + 235,
                quickBtnWidth,
                quickBtnHeight
            );
        }
    }
    
    // =========================================================================
    // PAINTING
    // =========================================================================
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw animated background
        drawBackground(g2d);
        
        // Draw floating icons
        drawFloatingIcons(g2d);
        
        // Draw particles
        drawParticles(g2d);
        
        // Draw login panel background
        drawLoginPanel(g2d);
        
        // Draw title
        drawTitle(g2d);
        
        // Draw Super Idol preview
        drawSuperIdolPreview(g2d);
        
        // Draw labels
        drawLabels(g2d);
        
        g2d.dispose();
    }
    
    /**
     * Draws the animated gradient background.
     */
    private void drawBackground(Graphics2D g2d) {
        // Animated gradient
        float offset = (float) (Math.sin(titlePulse * 0.5) * 0.1);
        
        GradientPaint gradient = new GradientPaint(
            0, 0, BACKGROUND_DARK,
            getWidth(), getHeight(), 
            new Color(
                (int) Math.min(255, BACKGROUND_LIGHT.getRed() + offset * 20),
                (int) Math.min(255, BACKGROUND_LIGHT.getGreen() + offset * 20),
                (int) Math.min(255, BACKGROUND_LIGHT.getBlue() + offset * 30)
            )
        );
        
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Grid pattern
        g2d.setColor(new Color(255, 255, 255, 10));
        for (int x = 0; x < getWidth(); x += 50) {
            g2d.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += 50) {
            g2d.drawLine(0, y, getWidth(), y);
        }
    }
    
    /**
     * Draws floating background icons.
     */
    private void drawFloatingIcons(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        
        for (FloatingIcon icon : floatingIcons) {
            int alpha = (int) (30 + Math.sin(titlePulse + icon.x * 0.01) * 15);
            g2d.setColor(new Color(255, 200, 100, Math.max(0, Math.min(255, alpha))));
            
            AffineTransform old = g2d.getTransform();
            g2d.translate(icon.x, icon.y);
            g2d.rotate(icon.angle);
            g2d.drawString(icon.symbol, 0, 0);
            g2d.setTransform(old);
        }
    }
    
    /**
     * Draws particles.
     */
    private void drawParticles(Graphics2D g2d) {
        for (Particle p : particles) {
            int alpha = (int) (p.alpha * 255);
            if (alpha > 0 && alpha <= 255) {
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
     * Draws the login panel background.
     */
    private void drawLoginPanel(Graphics2D g2d) {
        int panelWidth = 340;
        int panelHeight = 320;
        int x = getWidth() / 2 - panelWidth / 2;
        int y = getHeight() / 2 - 60;
        
        // Panel shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillRoundRect(x + 5, y + 5, panelWidth, panelHeight, 20, 20);
        
        // Panel background
        g2d.setColor(PANEL_BG);
        g2d.fillRoundRect(x, y, panelWidth, panelHeight, 20, 20);
        
        // Panel border
        g2d.setColor(new Color(70, 70, 80));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(x, y, panelWidth, panelHeight, 20, 20);
        
        // Quick login label
        g2d.setColor(TEXT_GRAY);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        String quickText = "Quick Login:";
        int textWidth = g2d.getFontMetrics().stringWidth(quickText);
        g2d.drawString(quickText, getWidth() / 2 - textWidth / 2, y + panelHeight - 45);
    }
    
    /**
     * Draws the animated title.
     */
    private void drawTitle(Graphics2D g2d) {
        // Title glow effect
        float glowIntensity = (float) (0.5 + Math.sin(titlePulse) * 0.3);
        
        String title = "SUPER IDOL CLICKER";
        String subtitle = "The Smile Sweeter Than Honey";
        
        g2d.setFont(new Font("Impact", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        int titleX = getWidth() / 2 - titleWidth / 2;
        int titleY = getHeight() / 2 - 180;
        
        // Title glow
        g2d.setColor(new Color(255, 100, 50, (int) (glowIntensity * 100)));
        for (int i = 1; i <= 3; i++) {
            g2d.drawString(title, titleX - i, titleY - i);
            g2d.drawString(title, titleX + i, titleY + i);
        }
        
        // Title main
        g2d.setColor(ACCENT_GOLD);
        g2d.drawString(title, titleX, titleY);
        
        // Subtitle
        g2d.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        fm = g2d.getFontMetrics();
        int subWidth = fm.stringWidth(subtitle);
        g2d.setColor(TEXT_GRAY);
        g2d.drawString(subtitle, getWidth() / 2 - subWidth / 2, titleY + 35);
    }
    
    /**
     * Draws the Super Idol preview image.
     */
    private void drawSuperIdolPreview(Graphics2D g2d) {
        if (superIdolImage != null) {
            int imgSize = 100;
            int x = getWidth() / 2 - imgSize / 2;
            int y = getHeight() / 2 - 140;
            
            // Pulsing scale effect
            double scale = 1.0 + Math.sin(titlePulse * 2) * 0.03;
            int scaledSize = (int) (imgSize * scale);
            int offsetX = (scaledSize - imgSize) / 2;
            int offsetY = (scaledSize - imgSize) / 2;
            
            // Glow effect
            g2d.setColor(new Color(255, 200, 50, 30));
            g2d.fillOval(x - offsetX - 10, y - offsetY - 10, scaledSize + 20, scaledSize + 20);
            
            // Draw image
            g2d.drawImage(superIdolImage, x - offsetX, y - offsetY, scaledSize, scaledSize, null);
        }
    }
    
    /**
     * Draws form labels.
     */
    private void drawLabels(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int startY = getHeight() / 2 - 20;
        
        g2d.setColor(TEXT_WHITE);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        g2d.drawString("Username", centerX - 135, startY - 8);
        g2d.drawString("Password", centerX - 135, startY + 52);
    }
    
    // =========================================================================
    // ANIMATION
    // =========================================================================
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Update animation time
        titlePulse += 0.05f;
        
        // Update floating icons
        for (FloatingIcon icon : floatingIcons) {
            icon.update(getWidth(), getHeight());
        }
        
        // Update particles
        particles.removeIf(Particle::isDead);
        for (Particle p : particles) {
            p.update();
        }
        
        // Spawn new particles occasionally
        if (random.nextDouble() < 0.1) {
            particles.add(new Particle(
                random.nextDouble() * getWidth(),
                getHeight() + 10,
                random
            ));
        }
        
        repaint();
    }
    
    // =========================================================================
    // LOGIN
    // =========================================================================
    
    /**
     * Attempts to login with current credentials.
     */
    private void attemptLogin() {
        String username = usernameField.getText().trim();
        
        if (username.isEmpty()) {
            statusLabel.setForeground(ACCENT_RED);
            statusLabel.setText("Please enter a username");
            return;
        }
        
        // Generate password hash (simplified for demo)
        String passwordHash = "hashed_password_" +
            (username.contains("Jorgito") ? "1" :
             username.contains("Joel") ? "2" :
             username.contains("Iker") ? "3" :
             username.contains("Zaka") ? "4" : "0");
        
        if (loginCallback != null) {
            statusLabel.setForeground(new Color(100, 200, 100));
            statusLabel.setText("Logging in...");
            loginCallback.onLogin(username, passwordHash);
        }
    }
    
    /**
     * Sets the login callback.
     */
    public void setLoginCallback(LoginCallback callback) {
        this.loginCallback = callback;
    }
    
    /**
     * Shows an error message.
     */
    public void showError(String message) {
        statusLabel.setForeground(ACCENT_RED);
        statusLabel.setText(message);
    }
    
    /**
     * Shows a success message.
     */
    public void showSuccess(String message) {
        statusLabel.setForeground(new Color(100, 200, 100));
        statusLabel.setText(message);
    }
    
    // =========================================================================
    // CLEANUP
    // =========================================================================
    
    /**
     * Stops animation and releases resources.
     */
    public void dispose() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }
}
