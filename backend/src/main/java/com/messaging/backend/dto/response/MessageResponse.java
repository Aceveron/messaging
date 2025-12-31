/*
 * Message Response DTO (Data Transfer Object)
 * 
 * This class represents a message object in API responses.
 * Contains all message information including sender/receiver IDs and encrypted media metadata.
 * 
 * Fields:
 * - _id: Message unique identifier
 * - text: Message text content
 * - senderId: ID of the user who sent the message
 * - receiverId: ID of the user who receives the message
 * - mediaId, encryptedKey, iv, hash: Encrypted media metadata (if any)
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
    
    // Encrypted media metadata (null if no media)
    private String mediaId;              // UUID of encrypted media file
    private String encryptedKey;         // Base64-encoded AES key
    private String iv;                   // Base64-encoded IV
    private String hash;                 // SHA-256 hash
    private String mimeType;             // Original MIME type
    private Long fileSize;               // Encrypted blob size
    
    private LocalDateTime createdAt;     // Creation timestamp
    private LocalDateTime updatedAt;     // Last update timestamp
}
