/*
 * Login Request DTO (Data Transfer Object)
 * 
 * This class represents the request body for user login endpoint.
 * Contains credentials required for authentication.
 * 
 * Fields:
 * - email: User's email address
 * - password: User's password
 * 
 * Used in: POST /api/auth/login
 */
package com.messaging.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: generates no-args constructor
@AllArgsConstructor // Lombok: generates all-args constructor
public class LoginRequest {
    private String email;    // User's email address
    private String password; // User's password
}
