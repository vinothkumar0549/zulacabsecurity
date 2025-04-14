package com.example.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/zulacab";
    private static final String USER = "root";
    private static final String PASSWORD = "mysql";

    //  Load MySQL JDBC Driver (optional if using JDBC 4.0+)
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error loading MySQL Driver", e);
        }
    }

    // Method to get a new connection for each API call
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
