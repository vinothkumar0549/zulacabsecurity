package com.example.service;

import java.util.List;

import com.example.database.Storage;
import com.example.pojo.CabPositions;
import com.example.pojo.CustomerAck;
import com.example.pojo.Ride;
import com.example.pojo.TotalSummary;
import com.example.pojo.User;
import com.example.util.Role;

import jakarta.ws.rs.BadRequestException;

public class CabService {

    private Storage storage;

    public CabService(Storage storage){
        this.storage = storage;
    }

    public User login(String username, String password) {

        User user = storage.getUser(username);
        if(user == null){
            throw new BadRequestException("User Not Found");
        }

        if(! user.getEncryptedpassword().equals(password)) {
            throw new SecurityException("Invalid Password");
        }
        user.setEncryptedpassword(decrypt(password, 1));
        //user.setEncryptedpassword(null);
        return user;
    }

    public int register(User user, String adminusername, String adminpassword, String cablocation) {
        int locationid = 0;
        if(user.getRole() == Role.CAB) {
            User AdminUser = storage.getUser(adminusername);
            if(AdminUser == null || AdminUser.getRole() != Role.ADMIN){
                throw new BadRequestException("Admin Not Found");
            }
            if(! AdminUser.getEncryptedpassword().equals(adminpassword)) {
                throw new SecurityException("Invalid Admin Password");
            }
            locationid = storage.checkLocation(cablocation);
            if(locationid == 0){
                throw new IllegalArgumentException("Invalid Location");
            }
        }

        int id = storage.addUser(user);
        if( id != -1 && user.getRole() == Role.CAB){
            storage.addCabLocation(id, locationid);
        }
        return id;
    }

    public int addlocation(String adminusername, String adminpassword, String locationname, int distance){

        User AdminUser = storage.getUser(adminusername);
        if(AdminUser == null){
            throw new BadRequestException("Admin Not Found");
        }
        if(AdminUser.getRole() != Role.ADMIN){
            throw new BadRequestException("Access Denied");
        }
        if(! AdminUser.getEncryptedpassword().equals(adminpassword)) {
            throw new SecurityException("Invalid Admin Password");
        }
        
        int locationid = storage.addLocation(locationname, distance);
        if(locationid == -1){
            throw new BadRequestException("Location Name or Location Distance already exist");
        }
        return locationid;
    }

    public String removelocation(String adminusername, String adminpassword, String locationname, int distance){

        User AdminUser = storage.getUser(adminusername);
        if(AdminUser == null){
            throw new BadRequestException("Admin Not Found");
        }
        if(AdminUser.getRole() != Role.ADMIN){
            throw new BadRequestException("Access Denied");
        }
        if(! AdminUser.getEncryptedpassword().equals(adminpassword)) {
            throw new SecurityException("Invalid Admin Password");
        }
        
        return storage.removeLocation(locationname, distance);
    }

    public List<CabPositions> checkavailablecab(String customerusername, String customerpassword) {
        User customer = storage.getUser(customerusername);
        if(customer == null || customer.getRole() != Role.CUSTOMER){
            throw new BadRequestException("Access Denied");
        }
        if(! customer.getEncryptedpassword().equals(customerpassword)) {
            throw new SecurityException("Invalid Customer Password");
        }
        return storage.checkAvailableCab();
    }

    public CustomerAck bookcab(String customerusername, String customerpassword, String source, String destination) {

        User customer = storage.getUser(customerusername);

        if(customer == null || customer.getRole() != Role.CUSTOMER){
            throw new BadRequestException("Access Denied");
        }
        if(! customer.getEncryptedpassword().equals(customerpassword)) {
            throw new SecurityException("Invalid Customer Password");
        }
        if(storage.checkLocation(source) == 0 || storage.checkLocation(destination) == 0){
            throw new IllegalArgumentException("Invalid Source or Destination");
        }

        CustomerAck customerack = storage.getFreeCab(customer.getUserid(), source, destination);

        if(customerack == null){
            throw new IllegalStateException("No Cab Found");
        }

        //storage.updateCabPositions(cabid, storage.checkLocation(destination));

        return customerack;

    }

    public int confirmride(String customerusername, String customerpassword, int cabid, int distance, String source, String destination) {
        User customer = storage.getUser(customerusername);

        if(customer == null || customer.getRole() != Role.CUSTOMER){
            throw new BadRequestException("Access Denied");
        }
        if(! customer.getEncryptedpassword().equals(customerpassword)) {
            throw new SecurityException("Invalid Customer Password");
        }
        storage.addRideHistory(customer.getUserid(), cabid, distance, source, destination);
        storage.updateCabPositions(cabid, storage.checkLocation(destination));
        return cabid;    
    }

    public List<Ride> customerSummary(String customerusername, String customerpassword) {
        User customer = storage.getUser(customerusername);
        if(customer == null || customer.getRole() != Role.CUSTOMER){
            throw new BadRequestException("Access Denied");
        }
        if(! customer.getEncryptedpassword().equals(customerpassword)) {
            throw new SecurityException("Invalid Customer Password");
        }
        return storage.getCustomerRideSummary(customer.getUserid());
    }

    public List<Ride> cabSummary(String cabusername, String cabpassword) {
        User cab = storage.getUser(cabusername);
        if(cab == null || cab.getRole() != Role.CAB){
            throw new BadRequestException("Access Denied");
        }
        if(! cab.getEncryptedpassword().equals(cabpassword)) {
            throw new SecurityException("Invalid Customer Password");
        }
        return storage.getCabRideSummary(cab.getUserid());
    }

    public List<List<Ride>> getallcabsummary(String adminusername, String adminpassword){

        User admin = storage.getUser(adminusername);

        if(admin == null || admin.getRole() != Role.ADMIN){
            throw new BadRequestException("Access Denied");
        }
        if(! admin.getEncryptedpassword().equals(adminpassword)) {
            throw new SecurityException("Invalid Customer Password");
        }

        return storage.getAllCabRides();
    }

    public List<TotalSummary> gettotalcabsummary(String adminusername, String adminpassword){
        User admin = storage.getUser(adminusername);

        if(admin == null || admin.getRole() != Role.ADMIN){
            throw new BadRequestException("Access Denied");
        }
        if(! admin.getEncryptedpassword().equals(adminpassword)) {
            throw new SecurityException("Invalid Customer Password");
        }
        return storage.getTotalCabSummary();
    }

    public List<List<Ride>> getallcustomersummary(String adminusername, String adminpassword){

        User admin = storage.getUser(adminusername);

        if(admin == null || admin.getRole() != Role.ADMIN){
            throw new BadRequestException("Access Denied");
        }
        if(! admin.getEncryptedpassword().equals(adminpassword)) {
            throw new SecurityException("Invalid Customer Password");
        }

        return storage.getAllCustomerRides();
    }

    public List<TotalSummary> gettotalcustomersummary(String adminusername, String adminpassword){
        User admin = storage.getUser(adminusername);

        if(admin == null || admin.getRole() != Role.ADMIN){
            throw new BadRequestException("Access Denied");
        }
        if(! admin.getEncryptedpassword().equals(adminpassword)) {
            throw new SecurityException("Invalid Customer Password");
        }
        return storage.getTotalCustomerSummary();
    }

    public String encrypt(String password, int shift) {
        StringBuilder builder = new StringBuilder();

        for (char c : password.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                char base = Character.isUpperCase(c) ? 'A' : (Character.isLowerCase(c) ? 'a' : '0');
                int range = Character.isDigit(c) ? 10 : 26;
                builder.append((char) (base + (c - base + shift) % range));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public String decrypt(String encryptedPassword, int shift) {
        StringBuilder builder = new StringBuilder();
    
        for (char c : encryptedPassword.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                char base = Character.isUpperCase(c) ? 'A' : (Character.isLowerCase(c) ? 'a' : '0');
                int range = Character.isDigit(c) ? 10 : 26;
                builder.append((char) (base + (c - base - shift + range) % range));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

}



// String query = "SELECT cp.cabid,\r\n" +
//                         "ABS(src.distance - dest.distance) AS total_distance,\r\n" +
//                         "COUNT(rd.rideid) AS trip_count\r\n" +
//                         "FROM cabpositions cp\r\n" +
//                         "JOIN locations cl ON cp.locationid = cl.locationid\r\n" +
//                         "JOIN locations src ON src.locationname = ?\r\n" +
//                         "JOIN locations dest ON dest.locationname = ?\r\n" +
//                         "LEFT JOIN ridedetails rd ON cp.cabid = rd.cabid\r\n" +
//                         "WHERE cp.cabid != (SELECT cabid\r\n" +
//                         "FROM ridedetails\r\n" +
//                         "ORDER BY rideid DESC\r\n" +
//                         "LIMIT 1)\r\n" +
//                         "GROUP BY cp.cabid, cl.distance, src.distance, dest.distance\r\n" +
//                         "ORDER BY ABS(cl.distance - src.distance) ASC, trip_count ASC\r\n" +
//                         "LIMIT 1;";
