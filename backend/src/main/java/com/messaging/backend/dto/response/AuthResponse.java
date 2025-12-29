/*
 * Authentication Response DTO (Data Transfer Object)
 * 
 * This class represents the response body for authentication endpoints (register/login).
 * Returns user information after successful authentication.
 * Password is explicitly excluded from the response for security.
 * 
 * Fields:
 * - _id: User's unique identifier
 * - fullname: User's full name
 * - email: User's email address
 * - profilePic: URL to user's profile picture
 * 
 * JWT token is sent separately as an HTTP-only cookie, not in this response.
 * 
 * Used in: POST /api/auth/register, POST /api/auth/login
 */
package com.messaging.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: generates no-args constructor
@AllArgsConstructor // Lombok: generates all-args constructor
public class AuthResponse {
    private String _id;        // User's unique ID (MongoDB format)
    private String fullname;   // User's full name
    private String email;      // User's email address
    private String profilePic; // Profile picture URL
}
