package com.example.util;

import java.sql.*;

import io.github.cdimascio.dotenv.Dotenv;

public class ShardManager {

    private static Dotenv dotenv = Dotenv.configure().directory("C:\\Users\\Administrator\\Desktop\\zulacab\\cab\\.env").load();

    private final Connection lookupDbConnection;

    public ShardManager(Connection lookupDbConnection) {
        this.lookupDbConnection = lookupDbConnection;
    }

    public int getShardId(int userId) {
        String query = "SELECT shard_id FROM shard_lookup WHERE ? BETWEEN range_start AND range_end";
        try (PreparedStatement stmt = lookupDbConnection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println(rs.getInt("shard_id"));
                    return rs.getInt("shard_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return createNewShard(); // Create if not found
    }

    public Connection getShardConnection(int shardId) throws SQLException {
        String url = dotenv.get("SHARD_USER_DB") + shardId;
        return DriverManager.getConnection(url, "root", "mysql");
    }

    public int createNewShard() {
        System.out.println("new Shard created");
        int[] range = calculateNextRange();
        int start = range[0], end = range[1];

        String insertQuery = "INSERT INTO shard_lookup (range_start, range_end) VALUES (?, ?)";
        try (PreparedStatement stmt = lookupDbConnection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, start);
            stmt.setInt(2, end);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int shardId = rs.getInt(1);
                    createDatabaseForShard(shardId);
                    return shardId;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int[] calculateNextRange() {
        String query = "SELECT MAX(range_end) AS max_end FROM shard_lookup";
        try (Statement stmt = lookupDbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                int maxEnd = rs.getInt("max_end");
                int newStart = maxEnd + 1;
                int newEnd = newStart + 1; // Increment by 2 range (start, end)
                return new int[]{newStart, newEnd};
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new int[]{1, 2}; // First shard
    }

    private void createDatabaseForShard(int shardId) {
        String dbName = "users" + shardId;
        try (Statement stmt = lookupDbConnection.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
            createTablesInShard(shardId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTablesInShard(int shardId) {
        String db = "users" + shardId;
 
    
        String createQuery ="CREATE TABLE " + db + ".users (" +
                            "userid INT PRIMARY KEY, " +
                            "name VARCHAR(20), " +
                            "password VARCHAR(100) NOT NULL, "+
                            "age INT, " +
                            "gender ENUM('MALE','FEMALE'), " +
                            "role ENUM('CUSTOMER','CAB','ADMIN'), " +
                            "username VARCHAR(100) NOT NULL UNIQUE)";

        String createcustomerquery = "CREATE TABLE " + db + ".customerdetails ("+
                                     "customerid INT,"+
                                     "penalty INT,"+
                                     "date DATE,"+
                                     "FOREIGN KEY (customerid) REFERENCES users(userid))";

        try (Statement stmt = lookupDbConnection.createStatement()) {
            stmt.executeUpdate(createQuery);
            stmt.executeUpdate(createcustomerquery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (lookupDbConnection != null && !lookupDbConnection.isClosed()) {
                lookupDbConnection.close();
                System.out.println("Lookup connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
}
