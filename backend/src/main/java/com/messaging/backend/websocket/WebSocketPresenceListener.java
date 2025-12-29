package com.messaging.backend.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketPresenceListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final Map<String, String> userToSession = new ConcurrentHashMap<>();

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = accessor.getSessionAttributes();
        String sessionId = accessor.getSessionId();
        System.out.println("WebSocket connect - sessionId: " + sessionId);
        if (attrs != null) {
            System.out.println("Session attributes: " + attrs);
            Object uidObj = attrs.get("userId");
            if (uidObj != null) {
                String userId = uidObj.toString();
                System.out.println("User connected: " + userId + " with session: " + sessionId);
                userToSession.put(userId, sessionId);
                broadcastOnlineUsers();
            } else {
                System.out.println("WARNING: userId not found in session attributes");
            }
        } else {
            System.out.println("WARNING: Session attributes are null");
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        System.out.println("WebSocket disconnect - sessionId: " + sessionId);
        String toRemove = null;
        for (Map.Entry<String, String> e : userToSession.entrySet()) {
            if (sessionId.equals(e.getValue())) {
                toRemove = e.getKey();
                break;
            }
        }
        if (toRemove != null) {
            System.out.println("User disconnected: " + toRemove);
            userToSession.remove(toRemove);
            broadcastOnlineUsers();
        } else {
            System.out.println("WARNING: No userId found for disconnected session");
        }
    }

    private void broadcastOnlineUsers() {
        Set<String> online = userToSession.keySet();
        System.out.println("Broadcasting online users: " + online + " (total: " + online.size() + ")");
        messagingTemplate.convertAndSend("/topic/onlineUsers", online);
    }

    public Set<String> getOnlineUsers() {
        return userToSession.keySet();
    }
}
