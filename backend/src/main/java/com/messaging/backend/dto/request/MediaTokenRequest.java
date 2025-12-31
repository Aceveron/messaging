/*
 * Media Token Request DTO
 */
package com.messaging.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaTokenRequest {
    private String mediaId; // Media ID to generate token for
}
