package com.example.database;

import com.example.pojo.User;

public interface Storage {

    public User getUser(String username, String password);

    public int addUser(User user);
}
