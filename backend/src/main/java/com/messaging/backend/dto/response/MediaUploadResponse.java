/*
 * Media Upload Response DTO
 */
package com.messaging.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponse {
    private String mediaId; // UUID of uploaded media
    private Long fileSize; // Size in bytes
    private String mimeType; // MIME type
}
