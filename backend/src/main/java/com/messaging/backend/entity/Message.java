/*
 * Message Entity - MongoDB Document Model
 * 
 * Represents a message in the messaging application and maps to the "messages" collection in MongoDB.
 * This entity stores all message data exchanged between users, including text and encrypted media metadata.
 * 
 * Features:
 * - Text message content (optional)
 * - Encrypted media metadata (client-side encryption)
 * - Sender and receiver references (User IDs)
 * - Automatic timestamp tracking (createdAt, updatedAt)
 * - Support for both text-only and media messages
 * 
 * MongoDB Collection: messages
 */
package com.messaging.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: generates no-args constructor
@AllArgsConstructor // Lombok: generates all-args constructor
@Document(collection = "messages") // Maps this class to MongoDB "messages" collection
public class Message {

    @Id // Marks this field as the primary key (MongoDB _id)
    private String id;

    private String text; // Message text content (optional, can be null for media-only messages)

    private String senderId; // User ID of the message sender (references User collection)

    private String receiverId; // User ID of the message receiver (references User collection)

    // Encrypted media metadata (null if no media)
    private String mediaId; // UUID of encrypted media file
    private String encryptedKey; // Base64-encoded AES key for decryption
    private String iv; // Base64-encoded initialization vector
    private String hash; // SHA-256 hash of encrypted blob
    private String mimeType; // Original MIME type (image/jpeg, etc.)
    private Long fileSize; // Size of encrypted blob in bytes

    @CreatedDate // Automatically populated with creation timestamp
    private LocalDateTime createdAt;

    @LastModifiedDate // Automatically updated on document modification
    private LocalDateTime updatedAt;
}
