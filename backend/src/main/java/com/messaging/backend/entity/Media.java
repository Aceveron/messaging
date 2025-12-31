/*
 * Media Entity - MongoDB Document Model
 * 
 * Represents encrypted media metadata in the messaging application.
 * The actual encrypted blob is stored on disk, this entity only stores metadata.
 * 
 * Features:
 * - Tracks encrypted media files
 * - Stores upload/access information
 * - Supports TTL-based automatic deletion
 * - Access control and audit trail
 * 
 * MongoDB Collection: media
 */
package com.messaging.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "media")
public class Media {

    @Id
    private String id; // MongoDB ObjectId

    @Indexed(unique = true)
    private String mediaId; // UUID - unique identifier for the encrypted file

    private String uploaderId; // User who uploaded this media

    private String fileName; // Original filename (optional, for debugging)

    private String mimeType; // Original MIME type (image/jpeg, image/png, etc.)

    private Long fileSize; // Size of encrypted blob in bytes

    private String storagePath; // Relative path to encrypted file on disk

    private String hash; // SHA-256 hash of encrypted blob (for verification)

    @CreatedDate
    private LocalDateTime createdAt; // When uploaded

    @Indexed
    private LocalDateTime expiresAt; // TTL for automatic deletion (default: 30 days)

    // Access control
    private List<String> accessedBy = new ArrayList<>(); // UserIds who accessed this media

    private LocalDateTime lastAccessedAt; // Last download time
}
