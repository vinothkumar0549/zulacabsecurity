package com.example.websocket;

import java.util.*;
import java.util.concurrent.*;

import jakarta.websocket.OnOpen;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/chat/{roomId}")
public class ChatEndpoint {

    // Map<roomId, List<Session>>
    private static final Map<String, Set<Session>> chatRooms = new ConcurrentHashMap<>();


    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") String roomId) {
        System.out.println("Chat Connected Successfully with "+ roomId);

        chatRooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);

        System.out.println("Session" + chatRooms);

    }

    @OnMessage
    public void onMessage(String message, Session sender, @PathParam("roomId") String roomId) {
        System.out.println("Session" + chatRooms);
        System.out.println("Message:"+ message);
        System.out.println("Sender:"+ sender);
        System.out.println("Roomid:"+ roomId);

        // Broadcast to all users in the same room
        for (Session session : chatRooms.getOrDefault(roomId, Set.of())) {
            if (session.isOpen()) {
                System.out.println("CHECK");
                session.getAsyncRemote().sendText(message);
            }
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("roomId") String roomId) {
        System.out.println("CLOSE CONNECTION");
        Set<Session> roomSessions = chatRooms.get(roomId);
        if (roomSessions != null) {
            roomSessions.remove(session);
            if (roomSessions.isEmpty()) {
                chatRooms.remove(roomId);
            }
        }
    }
}
