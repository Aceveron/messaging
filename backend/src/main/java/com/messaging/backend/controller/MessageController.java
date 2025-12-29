/*
 * Message Controller
 * 
 * This REST controller handles all messaging-related HTTP endpoints:
 * - GET /api/messages/users - Get all users for sidebar (excluding logged-in user)
 * - GET /api/messages/:DmId - Get conversation messages between logged-in user and another user
 * - POST /api/messages/send/:DmId - Send a message to another user
 * 
 * All endpoints require authentication (JWT token).
 * The controller receives HTTP requests, delegates business logic to MessageService,
 * and returns appropriate HTTP responses with status codes.
 * 
 * Response status codes:
 * - 200 OK: Successful request
 * - 201 Created: Message sent successfully
 * - 400 Bad Request: Invalid input
 * - 401 Unauthorized: Not authenticated
 * - 500 Internal Server Error: Unexpected server error
 */
package com.messaging.backend.controller;

import com.messaging.backend.dto.request.SendMessageRequest;
import com.messaging.backend.dto.response.ErrorResponse;
import com.messaging.backend.dto.response.MessageResponse;
import com.messaging.backend.dto.response.UserResponse;
import com.messaging.backend.entity.User;
import com.messaging.backend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Marks this as a REST controller (combines @Controller and @ResponseBody)
@RequestMapping("/api/messages") // Base path for all endpoints in this controller
public class MessageController {

    @Autowired
    private MessageService messageService; // Service for messaging business logic

    /**
     * GET /api/messages/users
     * Fetches all users except the logged-in user for the sidebar
     * Requires authentication
     * 
     * Response: List of users
     * 
     * @param authentication Spring Security authentication object (injected)
     * @return 200 OK with list of users, or 500 with error message
     */
    @GetMapping("/users")
    public ResponseEntity<?> getSidebarUsers(Authentication authentication) {
        try {
            // Get authenticated user
            User user = (User) authentication.getPrincipal();
            
            // Call service to fetch users
            List<UserResponse> users = messageService.getSidebarUsers(user.getId());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            // Unexpected server error
            System.err.println("Error fetching sidebar users: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error"));
        }
    }

    /**
     * GET /api/messages/:DmId
     * Fetches all messages in a conversation between logged-in user and another user
     * Requires authentication
     * 
     * Path variable: DmId - ID of the other user
     * Response: List of messages (chronologically ordered)
     * 
     * @param DmId ID of the other user in the conversation
     * @param authentication Spring Security authentication object (injected)
     * @return 200 OK with list of messages, or 500 with error message
     */
    @GetMapping("/{DmId}")
    public ResponseEntity<?> getConversationMessages(@PathVariable String DmId,
                                                     Authentication authentication) {
        try {
            // Get authenticated user
            User user = (User) authentication.getPrincipal();
            
            // Call service to fetch messages
            List<MessageResponse> messages = messageService.getConversationMessages(user.getId(), DmId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            // Unexpected server error
            System.err.println("Error fetching direct messages: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error"));
        }
    }

    /**
     * POST /api/messages/send/:DmId
     * Sends a message from logged-in user to another user
     * Requires authentication
     * 
     * Path variable: DmId - ID of the message recipient
     * Request body: SendMessageRequest (text, image)
     * Response: Sent message details
     * 
     * @param DmId ID of the message recipient
     * @param request Send message request (text and/or image)
     * @param authentication Spring Security authentication object (injected)
     * @return 201 Created with message details, or 400/500 with error message
     */
    @PostMapping("/send/{DmId}")
    public ResponseEntity<?> sendMessage(@PathVariable String DmId,
                                        @RequestBody SendMessageRequest request,
                                        Authentication authentication) {
        try {
            // Get authenticated user (sender)
            User user = (User) authentication.getPrincipal();
            
            // Call service to send message
            MessageResponse messageResponse = messageService.sendMessage(user.getId(), DmId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(messageResponse);
        } catch (IllegalArgumentException e) {
            // Business logic error (validation failed)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            // Unexpected server error
            System.err.println("Error sending direct message: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error"));
        }
    }
}
