/*
 * Update Profile Request DTO (Data Transfer Object)
 * 
 * This class represents the request body for updating user profile picture.
 * Contains the base64 encoded image data to be uploaded to Cloudinary.
 * 
 * Fields:
 * - profilePic: Base64 encoded image string or URL
 * 
 * Used in: PUT /api/auth/profile
 */
package com.messaging.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: generates no-args constructor
@AllArgsConstructor // Lombok: generates all-args constructor
public class UpdateProfileRequest {
    private String profilePic; // Base64 encoded image or URL
}
