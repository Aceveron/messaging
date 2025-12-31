/*
 * Send Message Request DTO (Data Transfer Object)
 * 
 * This class represents the request body for sending a message.
 * A message can contain text, encrypted media metadata, or both.
 * 
 * Fields:
 * - text: Message text content (optional)
 * - mediaId, encryptedKey, iv, hash: Encrypted media metadata (optional)
 * 
 * At least one field (text or mediaId) should be present.
 * 
 * Used in: POST /api/messages/send/:DmId
 */
package com.messaging.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: generates no-args constructor
@AllArgsConstructor // Lombok: generates all-args constructor
public class SendMessageRequest {
    private String text;          // Message text content (optional)
    
    // Encrypted media metadata (optional)
    private String mediaId;       // UUID of encrypted media file
    private String encryptedKey;  // Base64-encoded AES key
    private String iv;            // Base64-encoded IV
    private String hash;          // SHA-256 hash
    private String mimeType;      // Original MIME type
    private Long fileSize;        // Encrypted blob size
}
