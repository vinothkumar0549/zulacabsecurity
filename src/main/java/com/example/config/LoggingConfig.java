package com.example.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.logging.LogManager;
import java.io.InputStream;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class LoggingConfig implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
            System.out.println("Logging initialized successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // @Override
    // public void contextDestroyed(ServletContextEvent sce) {
    //     // Nothing to clean up
    // }
}

