/*
 * Message Service
 * 
 * This service handles all messaging-related business logic including:
 * - Fetching all users for the sidebar (excluding the logged-in user)
 * - Retrieving conversation history between two users
 * - Sending new messages with optional encrypted media
 * - Real-time message broadcasting via WebSocket
 * 
 * Business logic flow:
 * - Validates input data and user existence
 * - Interacts with database through repositories
 * - Stores encrypted media metadata (no server-side decryption)
 * - Broadcasts messages in real-time to connected users
 * 
 * This service is called by the MessageController to process messaging requests.
 */
package com.messaging.backend.service;

import com.messaging.backend.dto.request.SendMessageRequest;
import com.messaging.backend.dto.response.MessageResponse;
import com.messaging.backend.dto.response.UserResponse;
import com.messaging.backend.entity.Message;
import com.messaging.backend.entity.User;
import com.messaging.backend.repository.MessageRepository;
import com.messaging.backend.repository.UserRepository;
import com.messaging.backend.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service // Marks this as a Spring service component
public class MessageService {

    @Autowired
    private UserRepository userRepository; // Database access for users

    @Autowired
    private MessageRepository messageRepository; // Database access for messages

    @Autowired
    private WebSocketHandler webSocketHandler; // WebSocket handler for real-time updates

    /**
     * Fetches all users except the logged-in user
     * Used to populate the sidebar with potential chat recipients
     * 
     * @param loggedInUserId ID of the currently logged-in user
     * @return List of users (excluding logged-in user)
     */
    public List<UserResponse> getSidebarUsers(String loggedInUserId) {
        // Fetch all users from database
        List<User> allUsers = userRepository.findAll();

        // Filter out the logged-in user and map to UserResponse
        return allUsers.stream()
                .filter(user -> !user.getId().equals(loggedInUserId)) // Exclude logged-in user
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getFullname(),
                        user.getEmail(),
                        user.getProfilePic()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all messages in a conversation between two users
     * Returns messages in chronological order (oldest to newest)
     * 
     * @param loggedInUserId ID of the logged-in user
     * @param otherUserId ID of the other user in the conversation
     * @return List of messages exchanged between the two users
     */
    public List<MessageResponse> getConversationMessages(String loggedInUserId, String otherUserId) {
        // Fetch messages between the two users (bidirectional)
        List<Message> messages = messageRepository.findConversationMessages(loggedInUserId, otherUserId);

        // Map messages to MessageResponse objects
        return messages.stream()
                .map(message -> new MessageResponse(
                        message.getId(),
                        message.getText(),
                        message.getSenderId(),
                        message.getReceiverId(),
                        message.getMediaId(),
                        message.getEncryptedKey(),
                        message.getIv(),
                        message.getHash(),
                        message.getMimeType(),
                        message.getFileSize(),
                        message.getCreatedAt(),
                        message.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Sends a new message from one user to another
     * 
     * Process:
     * - Validates that sender and receiver exist
     * - Stores encrypted media metadata (client already uploaded media)
     * - Saves message to database
     * - Broadcasts message to receiver via WebSocket (real-time)
     * - Returns the saved message
     * 
     * @param senderId ID of the user sending the message
     * @param receiverId ID of the user receiving the message
     * @param request Send message request containing text and/or encrypted media metadata
     * @return MessageResponse with the sent message details
     */
    @SuppressWarnings("null")
    public MessageResponse sendMessage(String senderId, String receiverId, SendMessageRequest request) {
        // Validate sender exists
        userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        // Validate receiver exists
        userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        // Create new message
        Message message = new Message();
        message.setText(request.getText());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        
        // Store encrypted media metadata (if provided)
        message.setMediaId(request.getMediaId());
        message.setEncryptedKey(request.getEncryptedKey());
        message.setIv(request.getIv());
        message.setHash(request.getHash());
        message.setMimeType(request.getMimeType());
        message.setFileSize(request.getFileSize());
        
        // Ensure real-time timestamps are set on creation
        LocalDateTime now = LocalDateTime.now();
        message.setCreatedAt(now);
        message.setUpdatedAt(now);

        // Save message to database
        Message savedMessage = messageRepository.save(message);

        // Create response object
        MessageResponse messageResponse = new MessageResponse(
                savedMessage.getId(),
                savedMessage.getText(),
                savedMessage.getSenderId(),
                savedMessage.getReceiverId(),
                savedMessage.getMediaId(),
                savedMessage.getEncryptedKey(),
                savedMessage.getIv(),
                savedMessage.getHash(),
                savedMessage.getMimeType(),
                savedMessage.getFileSize(),
                savedMessage.getCreatedAt(),
                savedMessage.getUpdatedAt()
        );

        // Broadcast message to receiver via WebSocket (real-time)
        webSocketHandler.sendMessageToUser(receiverId, messageResponse);

        // Return message response
        return messageResponse;
    }
}
