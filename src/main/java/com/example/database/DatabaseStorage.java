package com.example.database;

import java.sql.CallableStatement;
// import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
// import java.sql.Types;
import java.time.LocalDate;
// import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
// import java.util.Collections;
// import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
// import java.util.stream.Collectors;

import com.example.pojo.CabPositions;
import com.example.pojo.CustomerAck;
import com.example.pojo.Penalty;
import com.example.pojo.Ride;
import com.example.pojo.TotalSummary;
import com.example.pojo.User;
import com.example.util.DatabaseConnection;
import com.example.util.Gender;
import com.example.util.Role;
// import com.example.websocket.DriverSocket;
import com.example.websocket.DriverSocket;


public class DatabaseStorage implements Storage {

    private static int userid = 0;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private static final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Override
    public int addUser(User user) {
        String insertCredentials = "INSERT INTO credentials (userid, username) VALUES (?, ?)";
        String insertUser = "INSERT INTO users (userid, name, username, password, age, gender, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        userid++;

        // First insert into central credentials database
        try (Connection credConn = DatabaseConnection.getCredentialsConnection();
            PreparedStatement psCred = credConn.prepareStatement(insertCredentials)) {

            psCred.setInt(1, userid);
            psCred.setString(2, user.getUsername());

            int credVal = psCred.executeUpdate();

            if (credVal != 0) {
                // Only proceed to shard insert if credentials insert is successful
                try (Connection userConn = DatabaseConnection.getShardConnection(userid);
                    PreparedStatement psUser = userConn.prepareStatement(insertUser)) {

                    psUser.setInt(1, userid);
                    psUser.setString(2, user.getName());
                    psUser.setString(3, user.getUsername());
                    psUser.setString(4, user.getEncryptedpassword());
                    psUser.setLong(5, user.getAge());
                    psUser.setString(6, user.getGender().name());
                    psUser.setString(7, user.getRole().name());

                    int userVal = psUser.executeUpdate();

                    if (userVal != 0) {
                        return userid; // success
                    } else {
                        // Optional: rollback credentials insert manually or flag for cleanup
                        System.err.println("User insert failed after credentials insert. UserID: " + userid);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("User shard insert failed. UserID: " + userid);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Credentials insert failed. User not added.");
        }

        return -1;
    }

    // @Override
    // public int addUser(User user) {
    //     String insertuser = "INSERT INTO users (userid, name, username, password, age, gender, role) VALUES (?,?,?,?,?,?,?)";
    //     // int generatedUserId = -1;
    //     userid++;
    //     try (Connection connection = DatabaseConnection.getShardConnection(userid);
    //          PreparedStatement preparedStatementuser = connection.prepareStatement(insertuser)) {

    //         preparedStatementuser.setInt(1, userid);
    //         preparedStatementuser.setString(2, user.getName());
    //         preparedStatementuser.setString(3, user.getUsername());
    //         preparedStatementuser.setString(4, user.getEncryptedpassword());
    //         preparedStatementuser.setLong(5, user.getAge());
    //         preparedStatementuser.setString(6, user.getGender().name());
    //         preparedStatementuser.setString(7, user.getRole().name()); 

    //         int val = preparedStatementuser.executeUpdate();

    //          if (val != 0) {
    //             return userid;
    //         //     // Get the generated primary key (userid)
    //         //     try (ResultSet generatedKeys = preparedStatementuser.getGeneratedKeys()) {
    //         //         if (generatedKeys.next()) {
    //         //             generatedUserId = generatedKeys.getInt(1); // Retrieve the generated ID
    //         //         }
    //         //     }
    //         }

    //     } catch (SQLException e) {
    //         e.printStackTrace(); 
    //     }
    //     return -1;
    // }

    @Override
    public User getUser(String username) {
        String getUserIdQuery = "SELECT userid FROM credentials WHERE username = ?";
        String getUserDetailsQuery = "SELECT * FROM users WHERE userid = ?";

        int userId = -1;

        // Step 1: Get userid from credentials table
        try (Connection credentialsConn = DatabaseConnection.getCredentialsConnection();
            PreparedStatement ps = credentialsConn.prepareStatement(getUserIdQuery)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                userId = rs.getInt("userid");
            } else {
                return null; // Username not found
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        // Step 2: Use userid to get correct shard and fetch user details
        try (Connection userShardConn = DatabaseConnection.getShardConnection(userId);
            PreparedStatement psUser = userShardConn.prepareStatement(getUserDetailsQuery)) {

            psUser.setInt(1, userId);
            ResultSet rsUser = psUser.executeQuery();

            if (rsUser.next()) {
                return new User(
                    rsUser.getInt("userid"),
                    rsUser.getString("name"),
                    rsUser.getString("username"),
                    rsUser.getString("password"),
                    rsUser.getInt("age"),
                    Gender.valueOf(rsUser.getString("gender")),
                    Role.valueOf(rsUser.getString("role"))
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // Not found in shard
    }

    
    // @Override
    // public User getUser(String username) {
    //     String query = "SELECT * FROM users WHERE username = ?";

    //     try {
    //         for (Connection connection : DatabaseConnection.getAllUserShardConnections()) {
    //             try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {

    //                 preparedStatement.setString(1, username);
    //                 ResultSet result = preparedStatement.executeQuery();

    //                 if (result.next()) {
    //                     return new User(
    //                         result.getInt("userid"),
    //                         result.getString("name"),
    //                         result.getString("username"),
    //                         result.getString("password"),
    //                         result.getInt("age"),
    //                         Gender.valueOf(result.getString("gender")),
    //                         Role.valueOf(result.getString("role"))
    //                     );
    //                 }

    //             } catch (SQLException e) {
    //                 e.printStackTrace(); // You could log and continue
    //             } finally {
    //                 try {
    //                     if (connection != null && !connection.isClosed()) connection.close();
    //                 } catch (SQLException ex) {
    //                     ex.printStackTrace();
    //                 }
    //             }
    //         }
    //     } catch (SQLException e) {
    //         e.printStackTrace();
    //     }

    //      return null; // No user found in any shard
    //  }


    @Override
    public boolean login(int userid){
        String query = "INSERT INTO onlinestatus (userid) VALUES (?)";
        try (Connection connection = DatabaseConnection.getOnlineStatusConnection(); 
            PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userid);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean logout(int userid){
        String query = "DELETE FROM onlinestatus WHERE userid = ?";
        try (Connection connection = DatabaseConnection.getOnlineStatusConnection(); 
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userid);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean addCabLocation(int cabid, int  locationid, String cabtype) {

        String cabpositionquery = "INSERT INTO cabpositions(cabid, locationid, cabtype) VALUES(?,?,?)";

        try (Connection connection = DatabaseConnection.getCabPositionConnection();
             PreparedStatement preparedStatementcabposition = connection.prepareStatement(cabpositionquery)) {

            preparedStatementcabposition.setInt(1, cabid);
            preparedStatementcabposition.setInt(2, locationid);
            preparedStatementcabposition.setString(3, cabtype);
            int val = preparedStatementcabposition.executeUpdate();
            return val > 0;

        } catch (SQLException e) {
            e.printStackTrace(); 
        }

        return false;

    }

    public int checkLocation(String cablocation) {
        String locationquery = "SELECT locationid FROM locations WHERE locationname = ?";

        try (Connection connection = DatabaseConnection.getLocationConnection();
        PreparedStatement preparedStatementlocation = connection.prepareStatement(locationquery)) {

            preparedStatementlocation.setString(1, cablocation);
            ResultSet result = preparedStatementlocation.executeQuery();

            if (result.next()) {
                return result.getInt("locationid");
            }

        } catch (SQLException e) {
            e.printStackTrace(); 
        }

        return 0;
    }

    public int addLocation(String locationname, int distance) {
        String query = "INSERT INTO locations(locationname, distance) VALUES (?,?)";
        int generatedUserId = -1;

        try (Connection connection = DatabaseConnection.getLocationConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(query,  PreparedStatement.RETURN_GENERATED_KEYS)) {

       preparedStatement.setString(1, locationname);
       preparedStatement.setInt(2, distance);

       int val = preparedStatement.executeUpdate();

            if (val != 0) {
                // Get the generated primary key (userid)
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedUserId = generatedKeys.getInt(1); // Retrieve the generated ID
                    }
                }
            }
            return generatedUserId;

        } catch (SQLException e) {
            e.printStackTrace(); 
        }

        return -1;
    }

    public String removeLocation(String locationname, int distance) {
        try (Connection connection = DatabaseConnection.getLocationConnection();
            CallableStatement callablestatement = connection.prepareCall("{call remove_location(?, ?, ?)}")) {

            callablestatement.setString(1, locationname);
            callablestatement.setInt(2, distance);
            callablestatement.registerOutParameter(3, Types.VARCHAR);
            callablestatement.execute();

            return callablestatement.getString(3);

        } catch (SQLException e) {
            e.printStackTrace(); 
        }

        return null;
    }    

    public List<CabPositions> checkAvailableCab() {
        String query = "SELECT l.locationname, GROUP_CONCAT(c.cabid ORDER BY c.cabid SEPARATOR ',') AS cabids \r\n" +
               "FROM location.locations l \r\n" +
               "JOIN cabposition.cabpositions c ON l.locationid = c.locationid \r\n" + 
               "WHERE c.cabstatus = 'AVAILABLE' \r\n" +
               "GROUP BY l.locationname;";

        List<CabPositions> availablecabs = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getCabPositionConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                
            ResultSet result = preparedStatement.executeQuery();
                
            while (result.next()) {
                CabPositions availablecab = new CabPositions(
                    result.getString("locationname"),
                    result.getString("cabids")
                );
                availablecabs.add(availablecab);
            }
                
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        return availablecabs;
  
    }


    public CustomerAck getFreeCab(int customerid, String source, String destination, String cabtype, LocalDateTime customerdeparturetime, LocalDateTime customerarrivaltime) {
        String query = "SELECT cp.cabid, ABS(src.distance - dest.distance) AS total_distance, COUNT(rd.rideid) AS trip_count " +
            "FROM cabposition.cabpositions cp " +
            "JOIN onlinestatus.onlinestatus u ON cp.cabid = u.userid " +  // Join with users to check online status
            "JOIN location.locations cl ON cp.locationid = cl.locationid " +
            "JOIN (SELECT distance FROM location.locations WHERE locationname = ?) AS src " +
            "JOIN (SELECT distance FROM location.locations WHERE locationname = ?) AS dest " +
            "LEFT JOIN ridedetail.ridedetails rd ON cp.cabid = rd.cabid " +
            "WHERE cp.cabid != IFNULL((SELECT cabid FROM ridedetail.ridedetails ORDER BY rideid DESC LIMIT 1), -1) " +
            "AND cp.cabstatus = 'AVAILABLE' AND cp.cabtype = ? " +
            // "AND u.onlinestatus = TRUE "
            "AND cp.cabid NOT IN (SELECT cabid FROM ridedetail.ridedetails WHERE (arrivaltime <= ? OR departuretime >= ?)) " + // departuretime < ? AND arrivaltime > ?
            "GROUP BY cp.cabid, cl.distance " +
            "ORDER BY ABS(cl.distance - src.distance) ASC, trip_count ASC LIMIT 1 FOR UPDATE;";

        try (Connection connection = DatabaseConnection.getCabPositionConnection()) {
            connection.setAutoCommit(false); // Start a transaction
    
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, source);
                preparedStatement.setString(2, destination);
                preparedStatement.setString(3, cabtype);
                preparedStatement.setTimestamp(4, Timestamp.valueOf(customerarrivaltime));
                preparedStatement.setTimestamp(5, Timestamp.valueOf(customerdeparturetime));
                ResultSet result = preparedStatement.executeQuery();
    
                if (result.next()) {
                    int cabId = result.getInt("cabid");
    
                    // Update cab status to "WAIT"
                    String updateQuery = "UPDATE cabpositions SET cabstatus = 'WAIT' WHERE cabid = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                        updateStmt.setInt(1, cabId);
                        updateStmt.executeUpdate();
                    }
    
                    // Commit the transaction after successfully updating the cab status
                    connection.commit();
    
                    // Schedule a task to release the cab if not confirmed within 1 minute
                    scheduleAutoRelease(cabId, customerid);
    
                    System.out.println("Selected CAB ID: " + cabId);
                    return new CustomerAck(
                        cabId,
                        result.getInt("total_distance"),
                        (result.getInt("total_distance") * 10),
                        source,
                        destination
                    );
                }
            } catch (SQLException e) {
                connection.rollback(); // Rollback if something goes wrong
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        return null; // Return null if no cab was found

    }
    
    
    // Schedules auto-release of cab after timeout, with cabpositions sharded
    private void scheduleAutoRelease(int cabId, int customerId) {
        Runnable autoReleaseTask = () -> {

            try (
                Connection cabconnection = DatabaseConnection.getCabPositionConnection(); // Cab shard
                Connection customerShardConnection = DatabaseConnection.getShardConnection(customerId) 
            ) {
                // 1. Update cab status if still in WAIT
                String updateQuery = "UPDATE cabpositions SET cabstatus = 'AVAILABLE' WHERE cabid = ? AND cabstatus = 'WAIT'";
                try (PreparedStatement updateStmt = cabconnection.prepareStatement(updateQuery)) {
                    updateStmt.setInt(1, cabId);
                    int rowsUpdated = updateStmt.executeUpdate();

                    if (rowsUpdated > 0) {
                        System.out.println("Cab " + cabId + " has been automatically released.");

                        // Notify cab driver via WebSocket
                        DriverSocket.sendCloseRequest(String.valueOf(cabId));

                        // 2. Insert penalty into centralized customerdetails (or customer_penalty table)
                        String insertPenaltyQuery = "INSERT INTO customerdetails (customerid, penalty, date) VALUES (?, ?, ?)";
                        try (PreparedStatement penaltyStmt = customerShardConnection.prepareStatement(insertPenaltyQuery)) {
                            penaltyStmt.setInt(1, customerId);
                            penaltyStmt.setInt(2, 20);
                            penaltyStmt.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
                            penaltyStmt.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                scheduledTasks.remove(cabId); // Always clean up task
            }
        };

        // Schedule task after 2 minutes
        ScheduledFuture<?> future = scheduler.schedule(autoReleaseTask, 2, TimeUnit.MINUTES);
        scheduledTasks.put(cabId, future);
    }

    

    public boolean addRideHistory(int customerid, int cabid, int distance, String source, String destination, LocalDateTime departuretime, LocalDateTime arrivaltime) {
        String rideInsertQuery = "INSERT INTO ridedetails (customerid, cabid, source, destination, fare, commission, departuretime, arrivaltime) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String updateCabQuery = "UPDATE cabpositions SET cabstatus = 'AVAILABLE' WHERE cabid = ? AND cabstatus = 'WAIT'";
    
        boolean rideInserted = false;
    
        // 1. Insert into ridedetails
        try (
            Connection rideConn = DatabaseConnection.getRideDetailConnection();
            PreparedStatement rideStmt = rideConn.prepareStatement(rideInsertQuery)
        ) {
            rideStmt.setInt(1, customerid);
            rideStmt.setInt(2, cabid);
            rideStmt.setString(3, source);
            rideStmt.setString(4, destination);
            rideStmt.setInt(5, distance * 10); // fare
            rideStmt.setInt(6, distance * 3);  // commission
            rideStmt.setTimestamp(7, Timestamp.valueOf(departuretime));
            rideStmt.setTimestamp(8, Timestamp.valueOf(arrivaltime));
    
            int result = rideStmt.executeUpdate();
            rideInserted = result > 0;
    
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    
        if (rideInserted) {
            // 2. Update cabpositions
            try (
                Connection cabConn = DatabaseConnection.getCabPositionConnection();
                PreparedStatement updateStmt = cabConn.prepareStatement(updateCabQuery)
            ) {
                updateStmt.setInt(1, cabid);
                updateStmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                // Optional: log or schedule retry of cab status update
            }
    
            // 3. Cancel any scheduled WAIT task
            ScheduledFuture<?> future = scheduledTasks.remove(cabid);
            if (future != null) {
                future.cancel(false);
            }
    
            return true;
        }
    
        return false;
    }
    

    public boolean cancelRide(int cabid, int customerid) {
        String updateCabPositionQuery = "UPDATE cabpositions SET cabstatus = 'AVAILABLE' WHERE cabid = ? AND cabstatus = 'WAIT';";
        String insertPenaltyQuery = "INSERT INTO customerdetails (customerid, penalty, date) VALUES (?, ?, ?);";

        boolean updated = false;

        try (
            Connection cabConn = DatabaseConnection.getCabPositionConnection();
            PreparedStatement updateStmt = cabConn.prepareStatement(updateCabPositionQuery)
        ) {
            updateStmt.setInt(1, cabid);
            int updateResult = updateStmt.executeUpdate();
            if (updateResult > 0) {
                updated = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (updated) {
            try (
                Connection customerConn = DatabaseConnection.getShardConnection(customerid); // or getCustomerShardConnection()
                PreparedStatement penaltyStmt = customerConn.prepareStatement(insertPenaltyQuery)
            ) {
                penaltyStmt.setInt(1, customerid);
                penaltyStmt.setInt(2, 20);
                penaltyStmt.setDate(3, java.sql.Date.valueOf(LocalDate.now()));

                int penaltyInserted = penaltyStmt.executeUpdate();

                // Cancel scheduled auto-release if any
                ScheduledFuture<?> future = scheduledTasks.remove(cabid);
                if (future != null) {
                    future.cancel(false);
                }

                return penaltyInserted > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return false;
    }


    public boolean updateCabPositions(int cabid, int locationid) {

        String query = "UPDATE cabpositions SET locationid = ? WHERE cabid = ?";

        try (Connection connection = DatabaseConnection.getCabPositionConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, locationid);
            preparedStatement.setInt(2, cabid);

            int val = preparedStatement.executeUpdate();

            return val > 0;

        } catch (SQLException e) {
            e.printStackTrace(); 
        }

        return false;
    }

    public List<Ride> getCustomerRideSummary(int customerid) {
        String query = "SELECT source, destination, cabid, fare FROM ridedetails WHERE customerid = ?";
        List<Ride> rides = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getRideDetailConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, customerid);
            ResultSet result = preparedStatement.executeQuery();

            while (result.next()) {
                // Create a new Ride object for each row
                Ride ride = new Ride(
                    result.getInt("cabid"), // cabid
                    result.getString("source"), // source
                    result.getString("destination"), // destination
                    result.getInt("fare") // fare
                );
    
                // Add the ride to the list
                rides.add(ride);
            }

        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        return rides;
    }
    

    public List<Penalty> getPenalty(int customerid) {
        String query = "SELECT penalty, date FROM customerdetails WHERE customerid = ?";
        List<Penalty> penalties = new ArrayList<>();
        
        try (Connection connection = DatabaseConnection.getShardConnection(customerid);
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, customerid);
            ResultSet result = preparedStatement.executeQuery();

            while (result.next()) {
                Penalty penalty = new Penalty(
                    result.getInt("penalty"),
                    result.getObject("date", LocalDate.class)
                );
                penalties.add(penalty);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return penalties;
    }
    

    public List<Ride> getCabRideSummary(int cabid) {
        String query = "SELECT source, destination, customerid, fare, commission FROM ridedetails WHERE cabid = ?";
        List<Ride> rides = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getRideDetailConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, cabid);
            ResultSet result = preparedStatement.executeQuery();

            while (result.next()) {
                // Create a new Ride object for each row
                Ride ride = new Ride(
                    result.getInt("customerid"), // cabid
                    result.getString("source"), // source
                    result.getString("destination"), // destination
                    result.getInt("fare"), // fare
                    result.getInt("Commission")

                );
    
                // Add the ride to the list
                rides.add(ride);
            }

        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        return rides;
    }
    
    public List<List<Ride>> getAllCabRides() {
        String query = "SELECT cabid, customerid, source, destination, fare, commission FROM ridedetails ORDER BY cabid ASC";
        Map<Integer, List<Ride>> cabRideMap = new TreeMap<>(); // TreeMap keeps keys sorted

        try (Connection connection = DatabaseConnection.getRideDetailConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet result = preparedStatement.executeQuery()) {

            while (result.next()) {
                int cabId = result.getInt("cabid");
                Ride ride = new Ride(
                    result.getInt("customerid"),
                    result.getString("source"),
                    result.getString("destination"),
                    result.getInt("fare"),
                    result.getInt("commission")
                );

                cabRideMap.computeIfAbsent(cabId, k -> new ArrayList<>()).add(ride);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Convert the grouped values to List<List<Ride>>
        return new ArrayList<>(cabRideMap.values());
    }
    

    public List<TotalSummary> getTotalCabSummary() {
        String query = "SELECT cabid, COUNT(*) AS total_rides, SUM(fare) AS total_fare, SUM(commission) AS total_commission \r\n" + 
                        "FROM ridedetails GROUP BY cabid ORDER BY cabid ASC;";      
        List<TotalSummary> totalcabsummary = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getRideDetailConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
           
            ResultSet result = preparedStatement.executeQuery();
           
            while (result.next()) {
                // Create a new Ride object for each row
                TotalSummary totalSummary = new TotalSummary(
                    result.getInt("cabid"), // cabid
                    result.getInt("total_rides"), // source
                    result.getInt("total_fare"), // destination
                    result.getInt("total_commission")
           
                );
               
                totalcabsummary.add(totalSummary);
           }   
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        return totalcabsummary;
    }
    

    public List<List<Ride>> getAllCustomerRides() {
        String query = "SELECT cabid, customerid, source, destination, fare FROM ridedetails ORDER BY customerid ASC";
        Map<Integer, List<Ride>> customerRideMap = new TreeMap<>(); // TreeMap keeps keys sorted

        try (Connection connection = DatabaseConnection.getRideDetailConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet result = preparedStatement.executeQuery()) {

            while (result.next()) {
                int customerId = result.getInt("customerid");
                Ride ride = new Ride(
                    result.getInt("cabid"),
                    result.getString("source"),
                    result.getString("destination"),
                    result.getInt("fare")
                );

                customerRideMap.computeIfAbsent(customerId, k -> new ArrayList<>()).add(ride);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Convert the grouped values to List<List<Ride>>
        return new ArrayList<>(customerRideMap.values());
    }
    

    public List<TotalSummary> getTotalCustomerSummary() {
        String query = "SELECT customerid, COUNT(*) AS total_rides, SUM(fare) AS total_fare \r\n" + 
                        "FROM ridedetails GROUP BY customerid ORDER BY customerid ASC;";      
        List<TotalSummary> totalcustomersummary = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getRideDetailConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
           
            ResultSet result = preparedStatement.executeQuery();
           
            while (result.next()) {
                // Create a new Ride object for each row
                TotalSummary totalSummary = new TotalSummary(
                    result.getInt("customerid"), // cabid
                    result.getInt("total_rides"), // source
                    result.getInt("total_Fare") // destination           
                );
               
                totalcustomersummary.add(totalSummary);
           }   
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        return totalcustomersummary;
    }

}
