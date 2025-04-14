package com.example.service;

import com.example.database.Storage;
import com.example.pojo.User;
import com.example.util.Role;

import jakarta.ws.rs.BadRequestException;

public class CabService {

    private Storage storage;

    public CabService(Storage storage){
        this.storage = storage;
    }

    public User login(String username, String password) {

        User user = storage.getUser(username, password);

        if(user == null){
            throw new BadRequestException("User Not Found");
        }

        if(! user.getEncryptedpassword().equals(password)) {
            throw new SecurityException("Invalid Password");
        }

        user.setEncryptedpassword(null);
        return user;
    }

    public int register(User user, String adminusername, String adminpassword, String cablocation) {
        if(user.getRole() == Role.CAB) {
            User AdminUser = storage.getUser(adminusername, adminpassword);
            if(AdminUser == null || AdminUser.getRole() != Role.ADMIN){
                throw new BadRequestException("Admin Not Found");
            }
            if(! AdminUser.getEncryptedpassword().equals(adminpassword)) {
                throw new SecurityException("Invalid Admin Password");
            }
        }

        return storage.addUser(user);
    }

    public void bookcab(String username, String password, String source, String destination){
        User user = storage.getUser(username, password);
        if(user == null){
            throw new BadRequestException("Customer Not Found");
        }
        if(! user.getEncryptedpassword().equals(password)){
            throw new SecurityException("Invalid Customer Password");
        }

    }

}
