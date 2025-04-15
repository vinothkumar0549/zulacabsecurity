package com.example.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.example.pojo.CustomerAck;
import com.example.pojo.Ride;
import com.example.pojo.TotalSummary;
import com.example.pojo.User;
import com.example.util.DatabaseConnection;
import com.example.util.Gender;
import com.example.util.Role;

public class DatabaseStorage implements Storage {

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
    public boolean addCabLocation(int cabid, int  locationid) {

        String cabpositionquery = "INSERT INTO cabpositions(cabid, locationid) VALUES(?,?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatementcabposition = connection.prepareStatement(cabpositionquery)) {

            preparedStatementcabposition.setInt(1, cabid);
            preparedStatementcabposition.setInt(2, locationid);
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

    public CustomerAck getFreeCab(int customerid, String source, String destination) {
        
        String query = "SELECT cp.cabid,\r\n" +
                        "ABS(src.distance - dest.distance) AS total_distance,\r\n" +
                        "COUNT(rd.rideid) AS trip_count\r\n" +
                        "FROM cabpositions cp\r\n" +
                        "JOIN locations cl ON cp.locationid = cl.locationid\r\n" +
                        "JOIN locations src ON src.locationname = ?\r\n" +
                        "JOIN locations dest ON dest.locationname = ?\r\n" +
                        "LEFT JOIN ridedetails rd ON cp.cabid = rd.cabid\r\n" +
                        "WHERE cp.cabid != (SELECT cabid\r\n" +
                        "FROM ridedetails\r\n" +
                        "ORDER BY rideid DESC\r\n" +
                        "LIMIT 1)\r\n" +
                        "GROUP BY cp.cabid, cl.distance, src.distance, dest.distance\r\n" +
                        "ORDER BY ABS(cl.distance - src.distance) ASC, trip_count ASC\r\n" +
                        "LIMIT 1;";
 

        try (Connection connection = DatabaseConnection.getConnection();
            PreparedStatement preparedStatementlocation = connection.prepareStatement(query)) {
                
            preparedStatementlocation.setString(1, source);
            preparedStatementlocation.setString(2, destination);
            ResultSet result = preparedStatementlocation.executeQuery();
                
            if (result.next()) {
                return new CustomerAck(
                    result.getInt("cabid"),
                    result.getInt("total_distance"),
                    (result.getInt("total_distance") * 3),
                    source,
                    destination
                );


            }
                
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
                
        return null;
    }

    public boolean addRideHistory(int customerid, int cabid, int distance, String source, String destination){
        String query = "INSERT INTO ridedetails(customerid, cabid, source, destination, fare, commission) VALUES (?,?,?,?,?,?)";

        try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(query)) {

       preparedStatement.setInt(1, customerid);
       preparedStatement.setInt(2, cabid);
       preparedStatement.setString(3, source);
       preparedStatement.setString(4, destination);
       preparedStatement.setInt(5, distance * 10);
       preparedStatement.setInt(6, distance * 3);

       int val = preparedStatement.executeUpdate();

       return val > 0;

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
        String query = "SELECT cabid, source, destination, fare, commission FROM ridedetails ORDER BY cabid ASC";
        Map<Integer, List<Ride>> cabRideMap = new TreeMap<>(); // TreeMap keeps keys sorted

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet result = preparedStatement.executeQuery()) {

            while (result.next()) {
                int cabId = result.getInt("cabid");
                Ride ride = new Ride(
                    cabId,
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

        String query = "SELECT cabid, COUNT(*) AS total_rides, SUM(fare) AS total_fare, SUM(commission) AS total_commission\r\n" + 
                        "FROM ridedetails GROUP BY cabid ORDER BY cabid ASC;";      
        List<TotalSummary> totalcabsummary = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
           
            ResultSet result = preparedStatement.executeQuery();
           
            while (result.next()) {
                // Create a new Ride object for each row
                TotalSummary totalSummary = new TotalSummary(
                    result.getInt("cabid"), // cabid
                    result.getInt("trips"), // source
                    result.getInt("fare"), // destination
                    result.getInt("commission")
           
                );
               
                totalcabsummary.add(totalSummary);
           }   
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        return totalcabsummary;
        
    }

    public List<List<Ride>> getAllCustomerRides() {
        String query = "SELECT cabid, source, destination, fare FROM ridedetails ORDER BY customer ASC";
        Map<Integer, List<Ride>> customerRideMap = new TreeMap<>(); // TreeMap keeps keys sorted

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet result = preparedStatement.executeQuery()) {

            while (result.next()) {
                int cabId = result.getInt("customerid");
                Ride ride = new Ride(
                    cabId,
                    result.getString("source"),
                    result.getString("destination"),
                    result.getInt("fare")
                );

                customerRideMap.computeIfAbsent(cabId, k -> new ArrayList<>()).add(ride);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Convert the grouped values to List<List<Ride>>
        return new ArrayList<>(customerRideMap.values());
    }

    public List<TotalSummary> getTotalCustomerSummary() {

        String query = "SELECT customerid, COUNT(*) AS total_rides, SUM(fare) AS total_fare\r\n" + 
                        "FROM ridedetails GROUP BY customerid ORDER BY customerid ASC;";      
        List<TotalSummary> totalcustomersummary = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
           
            ResultSet result = preparedStatement.executeQuery();
           
            while (result.next()) {
                // Create a new Ride object for each row
                TotalSummary totalSummary = new TotalSummary(
                    result.getInt("cabid"), // cabid
                    result.getInt("trips"), // source
                    result.getInt("fare") // destination           
                );
               
                totalcustomersummary.add(totalSummary);
           }   
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        return totalcustomersummary;
        
    }


}
