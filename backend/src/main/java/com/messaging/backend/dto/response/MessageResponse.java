/*
 * Message Response DTO (Data Transfer Object)
 * 
 * This class represents a message object in API responses.
 * Contains all message information including sender/receiver IDs and timestamps.
 * 
 * Fields:
 * - _id: Message unique identifier
 * - text: Message text content
 * - senderId: ID of the user who sent the message
 * - receiverId: ID of the user who receives the message
 * - image: URL to attached image (if any)
 * - createdAt: Timestamp when message was created
 * - updatedAt: Timestamp when message was last updated
 * 
 * Used in: GET /api/messages/:DmId, POST /api/messages/send/:DmId
 */
package com.messaging.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: generates no-args constructor
@AllArgsConstructor // Lombok: generates all-args constructor
public class MessageResponse {
    private String _id;                  // Message unique ID (MongoDB format)
    private String text;                 // Message text content
    private String senderId;             // Sender's user ID
    private String receiverId;           // Receiver's user ID
    private String image;                // Image URL (if attached)
    private LocalDateTime createdAt;     // Creation timestamp
    private LocalDateTime updatedAt;     // Last update timestamp
}
