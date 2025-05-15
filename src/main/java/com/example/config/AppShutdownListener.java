package com.example.config;


import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;


import com.example.util.DatabaseConnection;
import java.sql.DriverManager;
import jakarta.servlet.annotation.WebListener;
import java.util.logging.Logger;


@WebListener
public class AppShutdownListener implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(AppShutdownListener.class.getName());

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            if (DatabaseConnection.getShardManager() != null) {
                DatabaseConnection.getShardManager().close();
            }

            DriverManager.deregisterDriver(DriverManager.getDriver("jdbc:mysql://localhost:3306/shard_lookup_db"));
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
            System.out.println("Properly closed and cleaned up on undeploy.");
            logger.info("the DB Connection is Closed Correctly");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // @Override
    // public void contextInitialized(ServletContextEvent sce) {
    //     // No-op
    // }
}

