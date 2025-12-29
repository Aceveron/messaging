/*
 * Error Response DTO (Data Transfer Object)
 * 
 * This class represents the standard error response format for the API.
 * Provides consistent error messaging across all endpoints.
 * 
 * Fields:
 * - message: Human-readable error message describing what went wrong
 * 
 * Used in: All error responses (400, 401, 500, etc.)
 */
package com.messaging.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: generates no-args constructor
@AllArgsConstructor // Lombok: generates all-args constructor
public class ErrorResponse {
    private String message; // Error message describing the issue
}
