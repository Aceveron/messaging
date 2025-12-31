/*
 * Media Token Response DTO
 */
package com.messaging.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaTokenResponse {
    private String token; // Short-lived JWT for media download
    private Long expiresIn; // Token expiry in seconds (30s)
}
