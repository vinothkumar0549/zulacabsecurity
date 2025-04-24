package com.example.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.example.pojo.CabPositions;
import com.example.pojo.CustomerAck;
import com.example.pojo.Penalty;
import com.example.pojo.Ride;
import com.example.pojo.TotalSummary;
import com.example.pojo.User;
import com.example.util.DatabaseConnection;
import com.example.util.Gender;
import com.example.util.Role;

public class DatabaseStorage implements Storage {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public int addUser(User user) {
        String insertuser = "INSERT INTO users (name, username, password, age, gender, role) VALUES (?,?,?,?,?,?)";
        int generatedUserId = -1;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatementuser = connection.prepareStatement(insertuser, PreparedStatement.RETURN_GENERATED_KEYS)) {

            preparedStatementuser.setString(1, user.getName());
            preparedStatementuser.setString(2, user.getUsername());
            preparedStatementuser.setString(3, user.getEncryptedpassword());
            preparedStatementuser.setLong(4, user.getAge());
            preparedStatementuser.setString(5, user.getGender().name());
            preparedStatementuser.setString(6, user.getRole().name()); 

            int val = preparedStatementuser.executeUpdate();

            if (val != 0) {
                // Get the generated primary key (userid)
                try (ResultSet generatedKeys = preparedStatementuser.getGeneratedKeys()) {
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

    @Override
    public User getUser(String username) {
        String query = "SELECT * FROM users WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, username);
            ResultSet result = preparedStatement.executeQuery();

            if (result.next()) {
                return new User(
                result.getInt("userid"), 
                result.getString("name"), 
                result.getString("username"),
                result.getString("password"),
                result.getInt("age"),
                Gender.valueOf(result.getString("gender")),
                Role.valueOf(result.getString("role")));  
            }

        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        return null;
    }

    @Override
    public boolean addCabLocation(int cabid, int  locationid, String cabtype) {

        String cabpositionquery = "INSERT INTO cabpositions(cabid, locationid, cabtype) VALUES(?,?,?)";

        try (Connection connection = DatabaseConnection.getConnection();
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

        try (Connection connection = DatabaseConnection.getConnection();
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

        try (Connection connection = DatabaseConnection.getConnection();
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

        try (Connection connection = DatabaseConnection.getConnection();
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
        String query = "SELECT l.locationname, GROUP_CONCAT(c.cabid ORDER BY c.cabid) AS cabids \r\n" +
                        "FROM locations l JOIN cabpositions c ON l.locationid = c.locationid \r\n" + 
                        "WHERE c.cabstatus = 'AVAILABLE' \r\n" +
                        "GROUP BY l.locationname;";

        List<CabPositions> availablecabs = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
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
            "FROM cabpositions cp " +
            "JOIN locations cl ON cp.locationid = cl.locationid " +
            "JOIN (SELECT distance FROM locations WHERE locationname = ?) AS src " +
            "JOIN (SELECT distance FROM locations WHERE locationname = ?) AS dest " +
            "LEFT JOIN ridedetails rd ON cp.cabid = rd.cabid " +
            "WHERE cp.cabid != IFNULL((SELECT cabid FROM ridedetails ORDER BY rideid DESC LIMIT 1), -1) " +
            "AND cp.cabstatus = 'AVAILABLE' AND cp.cabtype = ? " +
            "AND cp.cabid NOT IN (SELECT cabid FROM ridedetails WHERE (departuretime < ? AND arrivaltime > ?)) " +
            "GROUP BY cp.cabid, cl.distance " +
            "ORDER BY ABS(cl.distance - src.distance) ASC, trip_count ASC LIMIT 1 FOR UPDATE;";
    
        try (Connection connection = DatabaseConnection.getConnection()) {
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
    
    // Method to schedule auto release of cab after a timeout
    private void scheduleAutoRelease(int cabId, int customerid) {
        // Scheduled task to revert cab status after 1 minute
        Runnable autoReleaseTask = new Runnable() {
            @Override
            public void run() {
                try (Connection connection = DatabaseConnection.getConnection()) {
                    String updateQuery = "UPDATE cabpositions SET cabstatus = 'AVAILABLE' WHERE cabid = ? AND cabstatus = 'WAIT'";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                        updateStmt.setInt(1, cabId);
                        int rowsUpdated = updateStmt.executeUpdate();
                        if (rowsUpdated > 0) {
                            System.out.println("Cab " + cabId + " has been automatically released.");
                            String insertPenaltyQuery = "INSERT INTO customerdetails (customerid, penalty, date) VALUES (?, ?, ?);";
                            try (PreparedStatement penaltyStatement = connection.prepareStatement(insertPenaltyQuery)) {
                                penaltyStatement.setInt(1, customerid);
                                penaltyStatement.setInt(2, 20);
                                penaltyStatement.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));
            
                                penaltyStatement.executeUpdate();
                            }

                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        };
    
        // Schedule the auto-release task to run after 1 minute
        scheduler.schedule(autoReleaseTask, 1, TimeUnit.MINUTES);
    }
    

    public boolean addRideHistory(int customerid, int cabid, int distance, String source, String destination, LocalDateTime departuretime, LocalDateTime arrivaltime){
        String query = "INSERT INTO ridedetails(customerid, cabid, source, destination, fare, commission, departuretime, arrivaltime) VALUES (?,?,?,?,?,?,?,?)";

        try (Connection connection = DatabaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, customerid);
            preparedStatement.setInt(2, cabid);
            preparedStatement.setString(3, source);
            preparedStatement.setString(4, destination);
            preparedStatement.setInt(5, distance * 10);
            preparedStatement.setInt(6, distance * 3);
            preparedStatement.setTimestamp(7, Timestamp.valueOf(departuretime));
            preparedStatement.setTimestamp(8, Timestamp.valueOf(arrivaltime));

            int val = preparedStatement.executeUpdate();

            String updateQuery = "UPDATE cabpositions SET cabstatus = 'AVAILABLE' WHERE cabid = ? AND cabstatus = 'WAIT';";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                updateStmt.setInt(1,cabid);
                updateStmt.executeUpdate();
            }

            return val > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean cancelRide(int cabid, int customerid) {
        String updatecabpositionquery = "UPDATE cabpositions SET cabstatus = 'AVAILABLE' WHERE cabid = ? AND cabstatus = 'WAIT';";
        String insertPenaltyQuery = "INSERT INTO customerdetails (customerid, penalty, date) VALUES (?, ?, ?);";


        try (Connection connection = DatabaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(updatecabpositionquery)) {
            
                preparedStatement.setInt(1, cabid);

            int val = preparedStatement.executeUpdate();

            if (val > 0) {
                // If the cab status was updated successfully, insert the penalty record
                try (PreparedStatement penaltyStatement = connection.prepareStatement(insertPenaltyQuery)) {
                    penaltyStatement.setInt(1, customerid);
                    penaltyStatement.setInt(2, 20);
                    penaltyStatement.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));

                    int penaltyResult = penaltyStatement.executeUpdate();
                    return penaltyResult > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateCabPositions(int cabid, int locationid) {

        String query = "UPDATE cabpositions SET locationid = ? WHERE cabid = ?";

        try (Connection connection = DatabaseConnection.getConnection();
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

    public List<Ride> getCustomerRideSummary(int customerid){
        String query = "SELECT source, destination, cabid, fare FROM ridedetails WHERE customerid = ?";
        List<Ride> rides = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
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
        String query = "SELECT penalty, date FROM  customerdetails WHERE customerid = ?";
        List<Penalty> penalties = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, customerid);
            ResultSet result = preparedStatement.executeQuery();

            while (result.next()) {
                // Create a new Ride object for each row
                Penalty penalty = new Penalty(
                    result.getInt("penalty"), // cabid
                    result.getDate("date") // source
                );
    
                // Add the ride to the list
                penalties.add(penalty);
            }

        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        return penalties;
    }

    public List<Ride> getCabRideSummary(int cabid){
        String query = "SELECT source, destination, customerid, fare, commission FROM ridedetails WHERE cabid = ?";
        List<Ride> rides = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
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

        try (Connection connection = DatabaseConnection.getConnection();
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
        try (Connection connection = DatabaseConnection.getConnection();
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

        try (Connection connection = DatabaseConnection.getConnection();
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
        try (Connection connection = DatabaseConnection.getConnection();
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
