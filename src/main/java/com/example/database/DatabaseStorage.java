package com.example.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.pojo.User;
import com.example.util.DatabaseConnection;
import com.example.util.Gender;
import com.example.util.Role;

public class DatabaseStorage implements Storage {

    @Override
    public int addUser(User user) {
        String insertuser = "INSERT INTO users (name, username, password, age, gender, role) VALUES (?,?,?,?,?,?)";
        String cabposition = "INSERT INTO cabpositions (cabid, locationname) VALUES (?,?)";
        int generatedUserId = -1;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatementuser = connection.prepareStatement(insertuser, PreparedStatement.RETURN_GENERATED_KEYS);
             PreparedStatement preparedStatementcabposition = connection.prepareStatement(cabposition)) {

            preparedStatementuser.setString(1, user.getName());
            preparedStatementuser.setString(2, user.getUsername());
            preparedStatementuser.setString(3, user.getEncryptedpassword());
            preparedStatementuser.setLong(4, user.getAge());
            preparedStatementuser.setString(5, user.getGender().name()); // for "ADMIN"
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

            if(user.getRole() == Role.ADMIN){
                preparedStatementcabposition.setInt(1,generatedUserId);
                preparedStatementcabposition.setString(2, cabposition);
            }
    
            return generatedUserId;

        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        return -1;
    }

    @Override
    public User getUser(String username, String password) {
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

    
    
}
