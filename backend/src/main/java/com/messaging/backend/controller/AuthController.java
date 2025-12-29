/*
 * Authentication Controller
 * 
 * This REST controller handles all authentication-related HTTP endpoints:
 * - POST /api/auth/register - Register a new user
 * - POST /api/auth/login - Login existing user
 * - POST /api/auth/logout - Logout current user
 * - PUT /api/auth/profile - Update user profile picture
 * - GET /api/auth/pulse - Check if user is authenticated
 * 
 * The controller receives HTTP requests, delegates business logic to AuthService,
 * and returns appropriate HTTP responses with status codes.
 * 
 * Response status codes:
 * - 200 OK: Successful request
 * - 201 Created: Successful registration
 * - 400 Bad Request: Invalid input or business logic error
 * - 401 Unauthorized: Not authenticated
 * - 500 Internal Server Error: Unexpected server error
 */
package com.messaging.backend.controller;

import com.messaging.backend.dto.request.LoginRequest;
import com.messaging.backend.dto.request.RegisterRequest;
import com.messaging.backend.dto.request.UpdateProfileRequest;
import com.messaging.backend.dto.response.AuthResponse;
import com.messaging.backend.dto.response.ErrorResponse;
import com.messaging.backend.entity.User;
import com.messaging.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController // Marks this as a REST controller (combines @Controller and @ResponseBody)
@RequestMapping("/api/auth") // Base path for all endpoints in this controller
public class AuthController {

    @Autowired
    private AuthService authService; // Service for authentication business logic

    /**
     * POST /api/auth/register
     * Registers a new user account
     * 
     * Request body: RegisterRequest (fullname, email, password)
     * Response: AuthResponse with user data + JWT cookie
     * 
     * @param request Registration details
     * @param response HTTP response (for setting cookie)
     * @return 201 Created with user data, or 400/500 with error message
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        try {
            // Call service to register user
            AuthResponse authResponse = authService.register(request, response);
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
        } catch (IllegalArgumentException e) {
            // Business logic error (validation failed)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            // Unexpected server error
            System.err.println("Error in register controller: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error"));
        }
    }

    /**
     * POST /api/auth/login
     * Authenticates a user and logs them in
     * 
     * Request body: LoginRequest (email, password)
     * Response: AuthResponse with user data + JWT cookie
     * 
     * @param request Login credentials
     * @param response HTTP response (for setting cookie)
     * @return 200 OK with user data, or 400/500 with error message
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            // Call service to authenticate user
            AuthResponse authResponse = authService.login(request, response);
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            // Authentication failed (invalid credentials)
            System.err.println("Error in login controller: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Invalid credentials"));
        }
    }

    /**
     * POST /api/auth/logout
     * Logs out the current user by clearing the JWT cookie
     * 
     * Response: Success message
     * 
     * @param response HTTP response (for clearing cookie)
     * @return 200 OK with success message, or 500 with error message
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        try {
            // Clear the token cookie by setting max age to 0
            Cookie cookie = new Cookie("token", "");
            cookie.setMaxAge(0); // Expire immediately
            cookie.setPath("/"); // Same path as when set
            response.addCookie(cookie);

            // Return success message
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "Logged out successfully");
            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            // Unexpected error
            System.err.println("Error in logout controller: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error"));
        }
    }

    /**
     * PUT /api/auth/profile
     * Updates the user's profile picture
     * Requires authentication (JWT token)
     * 
     * Request body: UpdateProfileRequest (profilePic)
     * Response: AuthResponse with updated user data
     * 
     * @param request Profile update details
     * @param authentication Spring Security authentication object (injected)
     * @return 200 OK with updated user data, or 400/401/500 with error message
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request,
                                          Authentication authentication) {
        try {
            // Get authenticated user from SecurityContext
            User user = (User) authentication.getPrincipal();
            
            // Call service to update profile
            AuthResponse authResponse = authService.updateProfile(user.getId(), request);
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {
            // Business logic error (validation failed)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            // Unexpected server error
            System.err.println("Error in profile controller: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error"));
        }
    }

    /**
     * GET /api/auth/pulse
     * Checks if the user is authenticated
        * Returns 200 with authenticated flag; if a JWT cookie is present and valid,
        * returns user data; otherwise returns authenticated=false.
     * 
     * Returns user data if authenticated, used to restore session on page refresh
     * 
     * @param authentication Spring Security authentication object (injected)
     * @return 200 OK with user data if authenticated, or 401 if not
     */
    @GetMapping("/pulse")
    public ResponseEntity<?> pulse(Authentication authentication) {
        try {
            // Check if user is authenticated
            if (authentication == null || !authentication.isAuthenticated()) {
                // Not authenticated
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("authenticated", false);
                responseBody.put("message", "Not authenticated");
                return ResponseEntity.ok(responseBody);
            }

            // Get authenticated user
            User user = (User) authentication.getPrincipal();
            
            // Return user data
                AuthResponse authResponse = new AuthResponse(
                    user.getId(),
                    user.getFullname(),
                    user.getEmail(),
                    user.getProfilePic()
                );
                return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            // Unexpected error
            System.err.println("Error in pulse controller: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error"));
        }
    }
}
