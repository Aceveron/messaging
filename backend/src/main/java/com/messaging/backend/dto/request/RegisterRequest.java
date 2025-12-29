/*
 * Register Request DTO (Data Transfer Object)
 * 
 * This class represents the request body for user registration endpoint.
 * Contains all required fields for creating a new user account.
 * 
 * Fields:
 * - fullname: User's full name (also serves as username)
 * - email: User's email address (used for login)
 * - password: User's password (will be hashed before storing)
 * 
 * Used in: POST /api/auth/register
 */
package com.messaging.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: generates no-args constructor
@AllArgsConstructor // Lombok: generates all-args constructor
public class RegisterRequest {
    private String fullname; // User's full name/username
    private String email;    // User's email address
    private String password; // User's plain text password (will be hashed)
}
