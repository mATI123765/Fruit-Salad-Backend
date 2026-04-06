package com.fruitsalad;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.fruitsalad.database.DatabaseConnection;
import com.fruitsalad.database.DatabaseManager;
import com.fruitsalad.game.GamePanel;
import com.fruitsalad.game.GameState;

/**
 * Main entry point for Super Idol Clicker.
 * Shows login screen and launches the game.
 * 
 * @author Fruit Salad Ltd.
 * @version 1.0
 */
public class Main {
    
    // Colors - Super Idol theme
    private static final Color MAIN_RED = new Color(200, 30, 30);
    private static final Color DARK_BG = new Color(30, 30, 40);
    private static final Color GOLD = new Color(255, 215, 0);
    
    // Database manager (shared)
    private static DatabaseManager dbManager;
    
    /**
     * Main entry point
     */
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Initialize database connection
        if (!initializeDatabase()) {
            JOptionPane.showMessageDialog(null, """
                                                Could not connect to database!
                                                
                                                Make sure:
                                                1. MySQL is running
                                                2. Database 'new_super_idol_u' exists
                                                3. Username and password are correct in DatabaseConnection.java""",
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Show login screen
        SwingUtilities.invokeLater(Main::showLoginScreen);
    }
    
    /**
     * Initializes the database connection and sets up the game
     */
    private static boolean initializeDatabase() {
        try {
            // Test connection
            if (!DatabaseConnection.testConnection()) {
                return false;
            }
            
            // Create database manager
            dbManager = new DatabaseManager();
            
            // Setup game in database (creates game entry and achievements if needed)
            dbManager.setupGame();
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Shows the login screen
     */
    private static void showLoginScreen() {
        // Create login frame
        JFrame loginFrame = new JFrame("Super Idol Clicker - 登录");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(450, 400);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setResizable(false);
        
        // Main panel with dark background
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(DARK_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        
        // ===== TITLE =====
        JLabel titleLabel = new JLabel("🎤 SUPER IDOL CLICKER 🎤");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 26));
        titleLabel.setForeground(GOLD);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Chinese subtitle
        JLabel chineseTitle = new JLabel("超级偶像的笑容都没你的甜");
        chineseTitle.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        chineseTitle.setForeground(Color.WHITE);
        chineseTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Database status
        JLabel dbStatus = new JLabel("Connected to: " + DatabaseConnection.getDatabaseName());
        dbStatus.setFont(new Font("Arial", Font.PLAIN, 11));
        dbStatus.setForeground(Color.GREEN);
        dbStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // ===== USERNAME FIELD =====
        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.WHITE);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextField usernameField = new JTextField(20);
        usernameField.setMaximumSize(new Dimension(280, 35));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // ===== PASSWORD FIELD =====
        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(Color.WHITE);
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setMaximumSize(new Dimension(280, 35));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // ===== LOGIN BUTTON =====
        JButton loginButton = new JButton("START GAME");
        loginButton.setFont(new Font("Arial", Font.BOLD, 16));
        loginButton.setBackground(MAIN_RED);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(250, 45));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // ===== STATUS LABEL =====
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // ===== QUICK LOGIN PANEL =====
        JPanel quickLoginPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        quickLoginPanel.setBackground(DARK_BG);
        quickLoginPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel quickLabel = new JLabel("Quick Login: ");
        quickLabel.setForeground(Color.GRAY);
        quickLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        quickLoginPanel.add(quickLabel);
        
        // Quick login buttons for test users
        String[] testUsers = {"Jorgito", "Mati_Papaya", "Iker_Huevo", "Zakaria"};
        for (String user : testUsers) {
            JButton quickBtn = new JButton(user.split("_")[0]);
            quickBtn.setFont(new Font("Arial", Font.PLAIN, 10));
            quickBtn.setMargin(new Insets(2, 5, 2, 5));
            quickBtn.addActionListener(e -> {
                usernameField.setText(user);
                loginButton.doClick();
            });
            quickLoginPanel.add(quickBtn);
        }
        
        // ===== LOGIN ACTION =====
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            
            if (username.isEmpty()) {
                statusLabel.setText("Please enter username!");
                return;
            }
            
            try {
                // For demo: password hash based on username
                // In production, use proper password hashing!
                String passwordHash = "hashed_password_" + 
                    (username.contains("Jorgito") ? "1" :
                     username.contains("Matias") ? "2" :
                     username.contains("Iker") ? "3" :
                     username.contains("Zaka") ? "4" : "0");
                
                int userId = dbManager.loginUser(username, passwordHash);
                
                if (userId > 0) {
                    statusLabel.setForeground(Color.GREEN);
                    statusLabel.setText("Login successful.");
                    
                    // Close login window
                    loginFrame.dispose();
                    
                    // Launch the game!
                    launchGame(userId, username);
                    
                } else {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Invalid username!");
                }
                
            } catch (SQLException ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Database error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        
        // Enter key triggers login
        passwordField.addActionListener(e -> loginButton.doClick());
        usernameField.addActionListener(e -> passwordField.requestFocus());
        
        // ===== ADD COMPONENTS =====
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(chineseTitle);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(dbStatus);
        mainPanel.add(Box.createVerticalStrut(25));
        mainPanel.add(userLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(usernameField);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(passLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(passwordField);
        mainPanel.add(Box.createVerticalStrut(25));
        mainPanel.add(loginButton);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(quickLoginPanel);
        
        // Show frame
        loginFrame.add(mainPanel);
        loginFrame.setVisible(true);
        
        // Focus username field
        usernameField.requestFocusInWindow();
    }
    
    /**
     * Launches the main game window
     */
    private static void launchGame(int userId, String username) {
        // Create game frame
        JFrame gameFrame = new JFrame("Super Idol Clicker - " + username + " - 超级偶像");
        gameFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        gameFrame.setSize(1100, 750);
        gameFrame.setLocationRelativeTo(null);
        gameFrame.setMinimumSize(new Dimension(900, 650));
        
        // Create game state
        GameState gameState = new GameState(userId, username);
        
        // Create game panel
        GamePanel gamePanel = new GamePanel(gameState, dbManager);
        
        // Add to frame
        gameFrame.add(gamePanel);
        
        // Handle window closing
        gameFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Ask to save
                int confirm = JOptionPane.showConfirmDialog(
                    gameFrame,
                    "Save and exit game?",
                    "Exit",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    // Save and exit
                    gamePanel.endSession();
                    gamePanel.stopTimers();
                    cleanup();
                    gameFrame.dispose();
                    System.exit(0);

                } else if (confirm == JOptionPane.NO_OPTION) {
                    // Exit without saving
                    gamePanel.stopTimers();
                    cleanup();
                    gameFrame.dispose();
                    System.exit(0);
                }
                // Cancel = do nothing
            }
        });
            
        // Show game
        gameFrame.setVisible(true);
        System.out.println("Game launched");
    }
    
    /**
     * Cleanup resources before exit
     */
    private static void cleanup() {
        DatabaseConnection.closeConnection();
        System.out.println("Goodbye!");
    }
}