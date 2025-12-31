/*
 * Media Controller
 * 
 * REST endpoints for encrypted media upload/download.
 * Implements token-based access control and streaming.
 * 
 * Endpoints:
 * - POST /api/media/upload - Upload encrypted media
 * - POST /api/media/token - Generate download token
 * - GET /api/media/{mediaId} - Download encrypted media (requires token)
 * - DELETE /api/media/{mediaId} - Delete media (owner only)
 * - HEAD /api/media/{mediaId} - Check if media exists
 */
package com.messaging.backend.controller;

import com.messaging.backend.dto.request.MediaTokenRequest;
import com.messaging.backend.dto.response.ErrorResponse;
import com.messaging.backend.dto.response.MediaTokenResponse;
import com.messaging.backend.dto.response.MediaUploadResponse;
import com.messaging.backend.entity.User;
import com.messaging.backend.service.MediaStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Objects;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Autowired
    private MediaStorageService mediaStorageService;

    /**
     * POST /api/media/upload
     * Uploads encrypted media file
     * 
     * Request:
     * - file: Encrypted blob (multipart)
     * - mimeType: Original MIME type
     * - hash: SHA-256 hash of encrypted data
     * 
     * Response: { mediaId, fileSize, mimeType }
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mimeType") String mimeType,
            @RequestParam("hash") String hash,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("File is empty"));
            }
            
            // Limit file size (10MB for encrypted blobs)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("File too large (max 10MB)"));
            }
            
            MediaUploadResponse response = mediaStorageService.uploadMedia(
                    file, user.getId(), mimeType, hash);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            System.err.println("Upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Upload failed"));
        }
    }

    /**
     * POST /api/media/token
     * Generates a short-lived download token (30s)
     * 
     * Request: { mediaId }
     * Response: { token, expiresIn }
     */
    @PostMapping("/token")
    public ResponseEntity<?> generateToken(
            @RequestBody MediaTokenRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            MediaTokenResponse response = mediaStorageService.generateDownloadToken(
                    request.getMediaId(), user.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Media not found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied"));
        } catch (Exception e) {
            System.err.println("Token generation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error"));
        }
    }

    /**
     * GET /api/media/{mediaId}?token=...
     * Downloads encrypted media file
     * 
     * Query param: token (30s TTL JWT)
     * Response: Binary stream (application/octet-stream)
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<?> downloadMedia(
            @PathVariable String mediaId,
            @RequestParam("token") String token,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            InputStream mediaStream = Objects.requireNonNull(
                mediaStorageService.downloadMedia(mediaId, token, user.getId()),
                "media stream");

            InputStreamResource resource = new InputStreamResource(mediaStream);

            return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_OCTET_STREAM))
                    .body(resource);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Media not found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Invalid or expired token"));
        } catch (Exception e) {
            System.err.println("Download failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Download failed"));
        }
    }

    /**
     * HEAD /api/media/{mediaId}
     * Checks if media exists (without downloading)
     * 
     * Response: 200 if exists, 404 if not found
     */
    @RequestMapping(value = "/{mediaId}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkMediaExists(@PathVariable String mediaId) {
        boolean exists = mediaStorageService.exists(mediaId);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * DELETE /api/media/{mediaId}
     * Deletes media (owner only)
     * 
     * Response: 204 No Content on success
     */
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<?> deleteMedia(
            @PathVariable String mediaId,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            mediaStorageService.deleteMedia(mediaId, user.getId());
            
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Media not found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only uploader can delete media"));
        } catch (Exception e) {
            System.err.println("Delete failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Delete failed"));
        }
    }
}
