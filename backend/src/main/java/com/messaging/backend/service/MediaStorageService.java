/*
 * Media Storage Service
 * 
 * Handles encrypted media file storage on local disk.
 * Manages file I/O, metadata tracking, and cleanup.
 * 
 * Security:
 * - Files are stored encrypted (never decrypted on server)
 * - Uses UUID for filenames (prevents path traversal)
 * - Token-based access control
 * - TTL-based automatic deletion
 */
package com.messaging.backend.service;

import com.messaging.backend.dto.response.MediaTokenResponse;
import com.messaging.backend.dto.response.MediaUploadResponse;
import com.messaging.backend.entity.Media;
import com.messaging.backend.repository.MediaRepository;
import com.messaging.backend.repository.MessageRepository;
import com.messaging.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MediaStorageService {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${media.storage.path:media-storage}")
    private String storageBasePath;

    @Value("${media.ttl.days:30}")
    private int mediaTTLDays;

    /**
     * Initializes storage directory on startup
     */
    public void init() {
        try {
            Path storagePath = Paths.get(storageBasePath);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                System.out.println("Created media storage directory: " + storagePath.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize media storage: " + e.getMessage());
        }
    }

    /**
     * Uploads encrypted media file
     * @param file Encrypted media file (multipart)
     * @param uploaderId User ID who uploaded
     * @param mimeType Original MIME type
     * @param hash SHA-256 hash of encrypted data
     * @return MediaUploadResponse with mediaId
     */
    public MediaUploadResponse uploadMedia(MultipartFile file, String uploaderId, String mimeType, String hash) {
        try {
            // Generate unique media ID
            String mediaId = UUID.randomUUID().toString();
            
            // Store encrypted file on disk
            String fileName = mediaId + ".enc";
            Path destinationPath = Paths.get(storageBasePath, fileName);
            
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Calculate expiry (30 days from now)
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(mediaTTLDays);
            
            // Save metadata to database
            Media media = new Media();
            media.setMediaId(mediaId);
            media.setUploaderId(uploaderId);
            media.setFileName(file.getOriginalFilename());
            media.setMimeType(mimeType);
            media.setFileSize(file.getSize());
            media.setStoragePath(fileName);
            media.setHash(hash);
            media.setCreatedAt(LocalDateTime.now());
            media.setExpiresAt(expiresAt);
            
            mediaRepository.save(media);
            
            System.out.println("Media uploaded: " + mediaId + " (" + file.getSize() + " bytes)");
            
            return new MediaUploadResponse(mediaId, file.getSize(), mimeType);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to store media file: " + e.getMessage());
        }
    }

    /**
     * Generates a short-lived download token for media access
     * @param mediaId Media ID
     * @param userId User requesting access
     * @return JWT token (30s expiry)
     */
    public MediaTokenResponse generateDownloadToken(String mediaId, String userId) {
        // Verify media exists
        mediaRepository.findByMediaId(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found"));

        // Verify user has access (sender or receiver)
        if (!hasAccess(mediaId, userId)) {
            throw new SecurityException("Access denied");
        }
        
        // Generate short-lived token (30 seconds)
        String token = jwtUtil.generateMediaToken(mediaId, userId);
        
        return new MediaTokenResponse(token, 30L);
    }

    /**
     * Checks if a user has access to a media file
     * User has access if they sent or received a message containing this media
     */
    private boolean hasAccess(String mediaId, String userId) {
        // Check if user sent or received a message with this mediaId
        return messageRepository.existsByMediaIdAndSenderIdOrReceiverId(mediaId, userId, userId);
    }

    /**
     * Retrieves encrypted media file as stream
     * @param mediaId Media ID
     * @param token Download token
     * @param userId User requesting download
     * @return File input stream
     */
    public InputStream downloadMedia(String mediaId, String token, String userId) {
        try {
            // Validate token
            if (!jwtUtil.validateMediaToken(token, mediaId, userId)) {
                throw new SecurityException("Invalid or expired token");
            }
            
            // Get media metadata
            Media media = mediaRepository.findByMediaId(mediaId)
                    .orElseThrow(() -> new IllegalArgumentException("Media not found"));
            
            // Update access log
            if (!media.getAccessedBy().contains(userId)) {
                media.getAccessedBy().add(userId);
            }
            media.setLastAccessedAt(LocalDateTime.now());
            mediaRepository.save(media);
            
            // Stream encrypted file
            Path filePath = Paths.get(storageBasePath, media.getStoragePath());
            File file = filePath.toFile();
            
            if (!file.exists()) {
                throw new IllegalArgumentException("Media file not found on disk");
            }
            
            System.out.println("Media downloaded: " + mediaId + " by user " + userId);
            
            return new FileInputStream(file);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to read media file: " + e.getMessage());
        }
    }

    /**
     * Deletes expired media files (TTL-based cleanup)
     * Called by scheduled job
     * @return Number of deleted files
     */
    public int deleteExpiredMedia() {
        List<Media> expiredMedia = mediaRepository.findByExpiresAtBefore(LocalDateTime.now());
        
        int deletedCount = 0;
        for (Media media : expiredMedia) {
            try {
                // Delete file from disk
                Path filePath = Paths.get(storageBasePath, media.getStoragePath());
                Files.deleteIfExists(filePath);
                
                // Delete metadata from database
                mediaRepository.delete(media);
                
                deletedCount++;
                System.out.println("Deleted expired media: " + media.getMediaId());
                
            } catch (IOException e) {
                System.err.println("Failed to delete media: " + media.getMediaId() + " - " + e.getMessage());
            }
        }
        
        if (deletedCount > 0) {
            System.out.println("Cleanup complete: " + deletedCount + " expired media files deleted");
        }
        
        return deletedCount;
    }

    /**
     * Deletes a specific media file (admin or owner only)
     */
    public void deleteMedia(String mediaId, String userId) {
        Media media = mediaRepository.findByMediaId(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found"));
        
        // Only uploader can delete
        if (!media.getUploaderId().equals(userId)) {
            throw new SecurityException("Only uploader can delete media");
        }
        
        try {
            // Delete file from disk
            Path filePath = Paths.get(storageBasePath, media.getStoragePath());
            Files.deleteIfExists(filePath);
            
            // Delete metadata
            mediaRepository.delete(media);
            
            System.out.println("Media deleted by user: " + mediaId);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete media: " + e.getMessage());
        }
    }

    /**
     * Gets media metadata (without file content)
     */
    public Media getMediaMetadata(String mediaId) {
        return mediaRepository.findByMediaId(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found"));
    }

    /**
     * Checks if media file exists
     */
    public boolean exists(String mediaId) {
        return mediaRepository.findByMediaId(mediaId).isPresent();
    }
}
