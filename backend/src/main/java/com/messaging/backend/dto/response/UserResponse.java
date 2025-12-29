/*
 * User Response DTO (Data Transfer Object)
 * 
 * This class represents a user object in API responses.
 * Used when returning user lists (e.g., sidebar users).
 * Password is explicitly excluded from the response for security.
 * 
 * Fields:
 * - _id: User's unique identifier
 * - fullname: User's full name
 * - email: User's email address
 * - profilePic: URL to user's profile picture
 * 
 * Used in: GET /api/messages/users (sidebar users list)
 */
package com.messaging.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: generates no-args constructor
@AllArgsConstructor // Lombok: generates all-args constructor
public class UserResponse {
    private String _id;        // User's unique ID (MongoDB format)
    private String fullname;   // User's full name
    private String email;      // User's email address
    private String profilePic; // Profile picture URL
}
