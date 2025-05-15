package com.example.util;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnection {

    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());


    private static Dotenv dotenv = Dotenv.configure().directory("C:\\Users\\Administrator\\Desktop\\zulacab\\cab\\.env").load();
    
    private static final String LOOKUP_DB_URL = dotenv.get("LOOKUP_DB_USER");   // "jdbc:mysql://localhost:3306/shard_lookup_db";
    private static final String DB_USER = dotenv.get("DB_USERNAME");
    private static final String DB_PASSWORD = dotenv.get("DB_PASSWORD");

    private static ShardManager shardManager;
    private static Connection lookupDbConnection;

    static {
        try {
            System.out.println("Class is created");
            logger.info("Try to create connection to the mysql database");
            Class.forName("com.mysql.cj.jdbc.Driver");
            lookupDbConnection = DriverManager.getConnection(LOOKUP_DB_URL, DB_USER, DB_PASSWORD);
            shardManager = new ShardManager(lookupDbConnection);

            // Register shutdown hook to close lookup connection
            // Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            //     if (shardManager != null) {
            //         shardManager.close();
            //     }
            //     logger.info("trying to the close the connection to the mysql database after server close");
            // }));

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Returns connection to appropriate shard based on user ID
    public static Connection getShardConnection(int userId) throws SQLException {
        // int shardId = shardManager.getShardId(userId);
        return shardManager.getShardConnection(shardManager.getShardId(userId));
    }

    public static List<Connection> getAllUserShardConnections() throws SQLException {
        List<Connection> shardConnections = new ArrayList<>();

        try (Statement stmt = lookupDbConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT shard_id FROM shard_lookup")) {

            while (rs.next()) {
                int shardId = rs.getInt("shard_id");
                String shardDbUrl = dotenv.get("SHARD_USER_DB") + shardId;
                Connection shardConnection = DriverManager.getConnection(shardDbUrl, DB_USER, DB_PASSWORD);
                shardConnections.add(shardConnection);
            }
        }

        return shardConnections;
    }
    
    public static Connection getLocationConnection() throws SQLException {
        return DriverManager.getConnection(dotenv.get("LOCATION_DB"),  DB_USER, DB_PASSWORD);
    }

    public static Connection getCabPositionConnection() throws SQLException {
        return DriverManager.getConnection(dotenv.get("CABPOSITION_DB"),  DB_USER, DB_PASSWORD);
    }

    public static Connection getRideDetailConnection() throws SQLException {
        return DriverManager.getConnection(dotenv.get("RIDEDETAIL_DB"),  DB_USER, DB_PASSWORD);
    }

    public static Connection getOnlineStatusConnection() throws SQLException {
        return DriverManager.getConnection(dotenv.get("ONLINESTATUS_DB"),  DB_USER, DB_PASSWORD);
    }

    public static ShardManager getShardManager() {
        return shardManager;
    }

}
