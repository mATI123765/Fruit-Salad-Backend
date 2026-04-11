package com.fruitsalad;

import com.fruitsalad.audio.SoundManager;
import com.fruitsalad.database.DatabaseConnection;
import com.fruitsalad.database.DatabaseManager;
import com.fruitsalad.game.GamePanel;
import com.fruitsalad.game.GameState;
import com.fruitsalad.ui.MenuPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;

/**
 * =============================================================================
 * SUPER IDOL CLICKER
 * =============================================================================
 * A Cookie Clicker-style idle game with Super Idol theme.
 * 
 * The smile sweeter than honey, the love hotter than 105 degrees.
 * 
 * @author  Fruit Salad Ltd. (Jorge, Joel, Iker, Zakaria)
 * @version 1.0.0
 * =============================================================================
 */
public class Main {
    
    // =========================================================================
    // CONSTANTS
    // =========================================================================
    
    private static final String GAME_TITLE = "Super Idol Clicker";
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    
    // =========================================================================
    // INSTANCE VARIABLES
    // =========================================================================
    
    private static JFrame mainFrame;
    private static MenuPanel menuPanel;
    private static DatabaseManager dbManager;
    
    // =========================================================================
    // ENTRY POINT
    // =========================================================================
    
    /**
     * Main entry point for the application.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Set dark theme for dialogs
            UIManager.put("OptionPane.background", new Color(40, 40, 50));
            UIManager.put("Panel.background", new Color(40, 40, 50));
            UIManager.put("OptionPane.messageForeground", Color.WHITE);
            
        } catch (Exception e) {
            // Continue with default look and feel
        }
        
        // Print startup message
        System.out.println("============================================");
        System.out.println("       SUPER IDOL CLICKER v1.0.0");
        System.out.println("         by Fruit Salad Ltd.");
        System.out.println("============================================");
        System.out.println();
        
        // Initialize database
        if (!initializeDatabase()) {
            showDatabaseError();
            return;
        }
        
        // Launch GUI on EDT
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }
    
    // =========================================================================
    // INITIALIZATION
    // =========================================================================
    
    /**
     * Initializes the database connection.
     * 
     * @return true if successful, false otherwise
     */
    private static boolean initializeDatabase() {
        System.out.println("[Main] Initializing database connection...");
        
        try {
            // Test connection
            if (!DatabaseConnection.testConnection()) {
                return false;
            }
            
            // Create database manager
            dbManager = new DatabaseManager();
            
            // Setup game data (creates game/achievements if needed)
            dbManager.setupGame();
            
            System.out.println("[Main] Database initialized successfully");
            return true;
            
        } catch (SQLException e) {
            System.err.println("[Main] Database error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Shows database connection error dialog.
     */
    private static void showDatabaseError() {
        String message = """
            Could not connect to database!
            
            Please ensure:
            1. MySQL server is running
            2. Database 'new_super_idol_u' exists
            3. Credentials in DatabaseConnection.java are correct
            
            The game requires a database connection to save progress.
            """;
        
        JOptionPane.showMessageDialog(
            null,
            message,
            "Database Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    // =========================================================================
    // GUI CREATION
    // =========================================================================
    
    /**
     * Creates and displays the main window.
     */
    private static void createAndShowGUI() {
        // Create main frame
        mainFrame = new JFrame(GAME_TITLE);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        mainFrame.setMinimumSize(new Dimension(1000, 700));
        mainFrame.setLocationRelativeTo(null);
        
        // Handle window closing
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });
        
        // Create and show menu
        showMenu();
        
        // Show frame
        mainFrame.setVisible(true);
        
        System.out.println("[Main] Application started");
    }
    
    /**
     * Shows the main menu.
     */
    private static void showMenu() {
        // Create menu panel
        menuPanel = new MenuPanel();
        
        // Set login callback
        menuPanel.setLoginCallback((username, passwordHash) -> {
            handleLogin(username, passwordHash);
        });
        
        // Add to frame
        mainFrame.getContentPane().removeAll();
        mainFrame.add(menuPanel);
        mainFrame.revalidate();
        mainFrame.repaint();
    }
    
    // =========================================================================
    // LOGIN HANDLING
    // =========================================================================
    
    /**
     * Handles user login attempt.
     * 
     * @param username The entered username
     * @param passwordHash The hashed password
     */
    private static void handleLogin(String username, String passwordHash) {
        try {
            int userId = dbManager.loginUser(username, passwordHash);
            
            if (userId > 0) {
                // Login successful
                System.out.println("[Main] Login successful for: " + username);
                menuPanel.showSuccess("Login successful!");
                
                // Small delay for visual feedback
                Timer delayTimer = new Timer(500, e -> {
                    launchGame(userId, username);
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
                
            } else {
                // Login failed
                System.out.println("[Main] Login failed for: " + username);
                menuPanel.showError("Invalid username or password");
            }
            
        } catch (SQLException e) {
            System.err.println("[Main] Login error: " + e.getMessage());
            menuPanel.showError("Database error occurred");
        }
    }
    
    // =========================================================================
    // GAME LAUNCH
    // =========================================================================
    
    /**
     * Launches the main game.
     * 
     * @param userId The logged in user's ID
     * @param username The logged in user's name
     */
    private static void launchGame(int userId, String username) {
        System.out.println("[Main] Launching game for: " + username);
        
        // Dispose menu
        if (menuPanel != null) {
            menuPanel.dispose();
        }
        
        // Update window title
        mainFrame.setTitle(GAME_TITLE + " - " + username);
        
        // Create game state
        GameState gameState = new GameState(userId, username);
        
        // Create game panel
        GamePanel gamePanel = new GamePanel(gameState, dbManager);
        
        // Replace content
        mainFrame.getContentPane().removeAll();
        mainFrame.add(gamePanel);
        mainFrame.revalidate();
        mainFrame.repaint();
        
        // Update close handler for game
        for (var listener : mainFrame.getWindowListeners()) {
            mainFrame.removeWindowListener(listener);
        }
        
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleGameExit(gamePanel);
            }
        });
        
        System.out.println("[Main] Game started successfully");
    }
    
    // =========================================================================
    // EXIT HANDLING
    // =========================================================================
    
    /**
     * Handles application exit from menu.
     */
    private static void handleExit() {
        int result = JOptionPane.showConfirmDialog(
            mainFrame,
            "Are you sure you want to exit?",
            "Exit Game",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            cleanup();
            System.exit(0);
        }
    }
    
    /**
     * Handles application exit from game.
     * 
     * @param gamePanel The active game panel
     */
    private static void handleGameExit(GamePanel gamePanel) {
        int result = JOptionPane.showConfirmDialog(
            mainFrame,
            "Save and exit game?",
            "Exit Game",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            // Save and exit
            gamePanel.endSession();
            gamePanel.stopTimers();
            cleanup();
            System.exit(0);
            
        } else if (result == JOptionPane.NO_OPTION) {
            // Exit without saving
            gamePanel.stopTimers();
            cleanup();
            System.exit(0);
        }
        // Cancel = do nothing
    }
    
    /**
     * Cleans up resources before exit.
     */
    private static void cleanup() {
        System.out.println("[Main] Cleaning up resources...");
        
        // Stop and dispose sound
        SoundManager.getInstance().dispose();
        
        // Close database connection
        DatabaseConnection.closeConnection();
        
        System.out.println("[Main] Goodbye!");
    }
}
