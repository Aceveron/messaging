/*
 * WebSocket Configuration
 * 
 * This class configures WebSocket support for the messaging application.
 * WebSocket enables real-time bidirectional communication between server and clients.
 * 
 * Configuration:
 * - Endpoint: /ws - WebSocket connection endpoint
 * - CORS: Allows connections from frontend origin
 * - SockJS: Fallback for browsers that don't support WebSocket
 * - STOMP: Simple Text Oriented Messaging Protocol over WebSocket
 * 
 * Message flow:
 * 1. Client connects to /ws endpoint
 * 2. Client subscribes to /topic/messages/{userId} to receive messages
 * 3. Server sends messages to specific user topics
 * 4. Client receives real-time message updates
 */
package com.messaging.backend.config;

import com.messaging.backend.websocket.UserHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration // Marks this as a configuration class
@EnableWebSocketMessageBroker // Enables WebSocket message handling with STOMP
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins}") // Injects allowed origins from application.properties
    private String allowedOrigins;

    @Autowired
    private UserHandshakeInterceptor userHandshakeInterceptor;

    /**
     * Configures the message broker for routing messages
     * 
     * - Simple broker: In-memory message broker for broadcasting messages
     * - Destination prefix: /topic for broadcast destinations, /user for user-specific
     * - Application prefix: /app for messages bound for @MessageMapping methods
     * 
     * @param registry Message broker registry
     */
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        // Enable simple broker with destination prefixes
        registry.enableSimpleBroker("/topic", "/user");
        
        // Set prefix for messages bound for @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        
        // Set prefix for user-specific destinations
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Registers STOMP endpoints for WebSocket connections
     * 
     * - Endpoint: /ws - WebSocket connection endpoint
     * - CORS: Configured with allowed origins from application.properties
     * - SockJS: Enabled for fallback support
     * 
     * @param registry STOMP endpoint registry
     */
    @Override
    @SuppressWarnings("null")
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        String[] origins = allowedOrigins != null ? allowedOrigins.split(",") : new String[]{"*"};
        
        // Native WebSocket endpoint (recommended for modern browsers)
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins)
                .addInterceptors(userHandshakeInterceptor);

        // SockJS fallback endpoint for older browsers
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOrigins(origins)
                .addInterceptors(userHandshakeInterceptor)
                .withSockJS();
    }
}
