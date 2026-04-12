package com.fruitsalad.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * =============================================================================
 * DATABASE CONNECTION
 * =============================================================================
 * Singleton class that manages the MySQL database connection.
 * Provides a single connection point for all database operations.
 * 
 * @author  Fruit Salad Ltd.
 * @version 1.0.0
 * =============================================================================
 */
public class DatabaseConnection {
    
    // =========================================================================
    // DATABASE CONFIGURATION
    // =========================================================================
    
    /** Database host address */
    private static final String HOST = "localhost";
    
    /** Database port */
    private static final String PORT = "3306";
    
    /** Database name */
    private static final String DATABASE = "new_super_idol_u";
    
    /** Database username */
    private static final String USER = "root";
    
    /** Database password - CHANGE THIS TO YOUR PASSWORD */
    private static final String PASSWORD = "brackyt76839@19";
    
    /** JDBC connection URL */
    private static final String URL = String.format(
        "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
        HOST, PORT, DATABASE
    );
    
    // =========================================================================
    // SINGLETON CONNECTION
    // =========================================================================
    
    private static Connection connection = null;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private DatabaseConnection() { }
    
    // =========================================================================
    // CONNECTION METHODS
    // =========================================================================
    
    /**
     * Gets the database connection.
     * Creates a new connection if one doesn't exist or has been closed.
     * 
     * @return Active database connection
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                // Load MySQL JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                // Establish connection
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("[Database] Connected to: " + DATABASE);
            }
            return connection;
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Driver not found. Ensure mysql-connector-j is in dependencies.", e);
        }
    }
    
    /**
     * Tests if the database connection can be established.
     * 
     * @return true if connection is successful, false otherwise
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            boolean valid = conn != null && !conn.isClosed();
            if (valid) {
                System.out.println("[Database] Connection test: PASSED");
            }
            return valid;
        } catch (SQLException e) {
            System.err.println("[Database] Connection test: FAILED - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Closes the database connection.
     * Should be called when the application exits.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("[Database] Connection closed.");
            } catch (SQLException e) {
                System.err.println("[Database] Error closing connection: " + e.getMessage());
            }
        }
    }
    
    // =========================================================================
    // GETTERS
    // =========================================================================
    
    /**
     * Gets the database name.
     * 
     * @return The name of the connected database
     */
    public static String getDatabaseName() {
        return DATABASE;
    }
    
    /**
     * Gets the connection host.
     * 
     * @return The database host address
     */
    public static String getHost() {
        return HOST;
    }
}