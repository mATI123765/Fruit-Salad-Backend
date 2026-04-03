package com.fruitsalad.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the database connection to the New Super Idol U database.
 * Uses singleton pattern to maintain a single connection.
 */
public class DatabaseConnection {

    /* DATABASE CONFIGURATION */
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DATABASE = "new_super_idol_u";
    private static final String USER = "root";
    private static final String PASSWORD = "brackyt76839@19";

    // Connection URL
    private static final String URL = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
        HOST, PORT, DATABASE
    );

    // Singleton connection instance
    private static Connection connection = null;

    /**
     * Private constructor - use getConnection() instead
     */
    private DatabaseConnection() {}

    /**
     * Gets the database connection.
     * Creates a new connection if one doesn't exist or is closed.
     * 
     * @return Active database connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Load the MySQL JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Create the connection
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Database connected succesfully.");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL Driver not found! Make sure mysql-connector-j is in dependencies.", e);
            }
        }
        return connection;
    }

    /**
     * Closes the database connection.
     * Call this when the application exits.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Tests if the database connection is working.
     * 
     * @return true if connection is successful, false otherwise
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("Connection test PASSED.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Connection test FAILED: " + e.getMessage() + ".");
        }
        return false;
    }

    /**
     * Gets the database name being used.
     * 
     * @return Database name
     */
    public static String getDatabaseName() {
        return DATABASE;
    }
}