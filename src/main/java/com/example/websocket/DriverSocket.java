package com.example.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/driver/{driverId}")
public class DriverSocket {

    // Map to store connected drivers (driverId -> session)
    private static ConcurrentHashMap<String, Session> driverSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("driverId") String driverId) {
        System.out.println("Driver connected: " + session);
        driverSessions.put(driverId, session);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("driverId") String driverId) {
        System.out.println("Received from driver " + driverId + ": " + message);
        // Usually drivers don’t send messages here unless responding.
    }

    @OnClose
    public void onClose(Session session, @PathParam("driverId") String driverId) {
        System.out.println("Driver disconnected: " + driverId);
        driverSessions.remove(driverId);
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("driverId") String driverId) {
        System.out.println("Error with driver " + driverId + ": " + throwable.getMessage());
    }

    // Static method to send a message to a driver
    public static void sendRideAssignment(String cabid, String roomId, String source, String destination) {
        Session session = driverSessions.get(cabid);
        if (session != null && session.isOpen()) {
            String jsonMessage = String.format(
                "{\"type\":\"ASSIGNED\", \"roomId\":\"%s\", \"source\":\"%s\", \"destination\":\"%s\" }",
                roomId, source, destination
            );
            try {
                session.getBasicRemote().sendText(jsonMessage);
                System.out.println("Ride assigned to Cab " + cabid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Cab " + cabid + " not connected.");
        }
    }

    public static void sendCloseRequest(String cabid) {
        Session session = driverSessions.get(cabid);
        if (session != null && session.isOpen()) {
            String jsonMessage = String.format(
                "{\"type\":\"CONNECTIONCLOSE\"}"
            );
            try {
                session.getBasicRemote().sendText(jsonMessage);
                System.out.println("CAB CLOSED " + cabid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(cabid +" not open connection");
        }
    }
}

