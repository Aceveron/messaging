/*
 * WebSocket Handler
 * 
 * This class manages WebSocket connections and real-time message broadcasting.
 * It tracks connected users and sends messages to specific recipients in real-time.
 * 
 * Features:
 * - Tracks online users with their WebSocket session IDs
 * - Broadcasts "online users" list when users connect/disconnect
 * - Sends messages to specific users via their WebSocket session
 * - Uses STOMP protocol for message routing
 * 
 * Real-time flow:
 * 1. User connects via WebSocket with their userId as query parameter
 * 2. User is added to online users map
 * 3. "Online users" event is broadcast to all connected clients
 * 4. When a message is sent, it's routed to the recipient's WebSocket session
 * 5. User disconnects, removed from map, "online users" event broadcast again
 */
package com.messaging.backend.websocket;

import com.messaging.backend.dto.response.MessageResponse;
import com.messaging.backend.entity.User;
import com.messaging.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Component // Marks this as a Spring component
public class WebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // Template for sending STOMP messages

    @Autowired
    private SimpUserRegistry simpUserRegistry; // Registry of connected STOMP users (by Principal name)

    @Autowired
    private UserRepository userRepository; // To resolve receiver principal name (email) from userId

    // Map to track online users: userId -> WebSocket session ID
    private final Map<String, String> userSocketMap = new ConcurrentHashMap<>();

    /**
     * Called when a new WebSocket connection is established
     * Extracts userId from query parameter and adds to online users map
     * Broadcasts updated online users list to all connected clients
     * 
     * @param session WebSocket session that was established
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        // Extract userId from query parameters
        var uri = session.getUri();
        String query = uri != null ? uri.getQuery() : null;
        String userId = extractUserId(query);
        
        if (userId != null) {
            // Add user to online users map
            userSocketMap.put(userId, session.getId());
            System.out.println("User connected: " + userId + " (session: " + session.getId() + ")");
            
            // Broadcast updated online users list
            broadcastOnlineUsers();
        }
    }

    /**
     * Called when a WebSocket connection is closed
     * Removes user from online users map
     * Broadcasts updated online users list to all connected clients
     * 
     * @param session WebSocket session that was closed
     * @param status Close status information
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        // Find and remove user from map
        String userId = getUserIdBySessionId(session.getId());
        
        if (userId != null) {
            userSocketMap.remove(userId);
            System.out.println("User disconnected: " + userId + " (session: " + session.getId() + ")");
            
            // Broadcast updated online users list
            broadcastOnlineUsers();
        }
    }

    /**
     * Sends a message to a specific user via WebSocket
     * If user is online, message is sent to their subscribed topic
     * 
     * @param userId ID of the user to send message to
     * @param message Message to send
     */
    @SuppressWarnings("null")
    public void sendMessageToUser(String userId, MessageResponse message) {
        // Resolve the receiver's Principal name used by STOMP user destinations
        User receiver = userRepository.findById(userId).orElse(null);
        if (receiver == null) {
            System.out.println("Receiver not found for userId: " + userId + " (message not sent)");
            return;
        }

        String principalName = receiver.getEmail();

        // Check online presence via SimpUserRegistry (by Principal name)
        boolean isOnline = simpUserRegistry.getUser(principalName) != null;
        if (!isOnline) {
            System.out.println("User not online: " + userId + " (" + principalName + ") (message not sent in real-time)");
            return;
        }

        // Route to the user's personal destination; client should subscribe to /user/topic/messages
        messagingTemplate.convertAndSendToUser(
                principalName,
                "/topic/messages",
                message
        );
        System.out.println("Message sent to userId: " + userId + " as principal: " + principalName);
    }

    /**
     * Broadcasts the list of online users to all connected clients
     * Sends to /topic/onlineUsers which all clients can subscribe to
     */
    @SuppressWarnings("null")
    private void broadcastOnlineUsers() {
        // Get set of all online user IDs
        Set<String> onlineUserIds = userSocketMap.keySet();
        
        // Broadcast to /topic/onlineUsers
        messagingTemplate.convertAndSend("/topic/onlineUsers", onlineUserIds);
        System.out.println("Broadcast online users: " + onlineUserIds.size() + " users online");
    }

    /**
     * Extracts userId from WebSocket query string
     * Query format: userId=123abc
     * 
     * @param query Query string from WebSocket URL
     * @return userId or null if not found
     */
    private String extractUserId(String query) {
        if (query != null && query.contains("userId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("userId=")) {
                    return param.substring("userId=".length());
                }
            }
        }
        return null;
    }

    /**
     * Finds userId by WebSocket session ID
     * Used when connection is closed to identify which user disconnected
     * 
     * @param sessionId WebSocket session ID
     * @return userId or null if not found
     */
    private String getUserIdBySessionId(String sessionId) {
        for (Map.Entry<String, String> entry : userSocketMap.entrySet()) {
            if (entry.getValue().equals(sessionId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Gets the WebSocket session ID for a specific user
     * 
     * @param userId User ID
     * @return Session ID or null if user is not connected
     */
    public String getReceiverSocketId(String userId) {
        return userSocketMap.get(userId);
    }
}
