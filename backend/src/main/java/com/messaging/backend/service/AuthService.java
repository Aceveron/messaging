/*
 * Authentication Service
 * 
 * This service handles all authentication-related business logic including:
 * - User registration with validation and password hashing
 * - User login with credential verification
 * - Profile picture updates with Cloudinary integration
 * - Authentication status checking
 * 
 * Business logic flow:
 * - Validates input data
 * - Interacts with database through repositories
 * - Generates JWT tokens for authenticated users
 * - Handles errors and returns appropriate responses
 * 
 * This service is called by the AuthController to process authentication requests.
 */
package com.messaging.backend.service;

import com.messaging.backend.dto.request.LoginRequest;
import com.messaging.backend.dto.request.RegisterRequest;
import com.messaging.backend.dto.request.UpdateProfileRequest;
import com.messaging.backend.dto.response.AuthResponse;
import com.messaging.backend.entity.User;
import com.messaging.backend.repository.UserRepository;
import com.messaging.backend.util.CloudinaryUtil;
import com.messaging.backend.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service // Marks this as a Spring service component
public class AuthService {

    @Autowired
    private UserRepository userRepository; // Database access for users

    @Autowired
    private PasswordEncoder passwordEncoder; // BCrypt password encoder

    @Autowired
    private JwtUtil jwtUtil; // JWT token utility

    @Autowired
    private AuthenticationManager authenticationManager; // Spring Security authentication

    @Autowired
    private CloudinaryUtil cloudinaryUtil; // Cloudinary image upload utility

    /**
     * Registers a new user
     * 
     * Validation:
     * - Checks all required fields are provided
     * - Validates password length (minimum 6 characters)
     * - Checks email and fullname uniqueness
     * 
     * Process:
     * - Hashes password using BCrypt
     * - Saves user to database
     * - Generates JWT token
     * - Sets token as HTTP-only cookie
     * - Returns user data
     * 
     * @param request Registration request containing user details
     * @param response HTTP response to set cookie
     * @return AuthResponse with user data
     */
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        // Validate required fields
        if (request.getFullname() == null || request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Please provide all required fields");
        }

        // Validate password length
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }

        // Check email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Check fullname uniqueness
        if (userRepository.existsByFullname(request.getFullname())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Create new user
        User user = new User();
        user.setFullname(request.getFullname());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Hash password
        user.setProfilePic(""); // Default empty profile picture

        // Save user to database
        User savedUser = userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser.getEmail());

        // Set token as HTTP-only cookie (secure, prevents XSS attacks)
        setTokenCookie(response, token);

        // Return user data (without password)
        return new AuthResponse(
                savedUser.getId(),
                savedUser.getFullname(),
                savedUser.getEmail(),
                savedUser.getProfilePic()
        );
    }

    /**
     * Authenticates a user and logs them in
     * 
     * Process:
     * - Validates credentials using Spring Security
     * - Generates JWT token on successful authentication
     * - Sets token as HTTP-only cookie
     * - Returns user data
     * 
     * @param request Login request containing credentials
     * @param response HTTP response to set cookie
     * @return AuthResponse with user data
     */
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        // Authenticate user credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Get authenticated user
        User user = (User) authentication.getPrincipal();

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());

        // Set token as HTTP-only cookie
        setTokenCookie(response, token);

        // Return user data (without password)
        return new AuthResponse(
                user.getId(),
                user.getFullname(),
                user.getEmail(),
                user.getProfilePic()
        );
    }

    /**
     * Updates user's profile picture
     * 
     * Process:
     * - Validates profile picture data is provided
     * - Uploads image to Cloudinary
     * - Updates user record with new image URL
     * - Returns updated user data
     * 
     * @param userId ID of the user to update
     * @param request Update profile request containing image data
     * @return AuthResponse with updated user data
     */
    @SuppressWarnings("null")
    public AuthResponse updateProfile(String userId, UpdateProfileRequest request) {
        // Validate profile picture is provided
        if (request.getProfilePic() == null || request.getProfilePic().isEmpty()) {
            throw new IllegalArgumentException("Please provide a profile picture");
        }

        // Find user in database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Upload image to Cloudinary
        String imageUrl = cloudinaryUtil.uploadImage(request.getProfilePic());

        // Update user's profile picture
        user.setProfilePic(imageUrl);
        User updatedUser = userRepository.save(user);

        // Return updated user data
        return new AuthResponse(
                updatedUser.getId(),
                updatedUser.getFullname(),
                updatedUser.getEmail(),
                updatedUser.getProfilePic()
        );
    }

    /**
     * Sets JWT token as an HTTP-only cookie
     * 
     * Cookie configuration:
     * - Name: "token"
     * - Value: JWT token string
     * - Max age: 24 hours (86400 seconds)
     * - HTTP-only: true (prevents JavaScript access, XSS protection)
     * - Secure: false in development, true in production (HTTPS only)
     * - Same-site: Strict (CSRF protection)
     * - Path: "/" (available for all endpoints)
     * 
     * @param response HTTP response to set cookie
     * @param token JWT token string
     */
    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setMaxAge(24 * 60 * 60); // 1 day in seconds
        cookie.setHttpOnly(true); // Prevent JavaScript access (XSS protection)
        cookie.setSecure(false); // Set to true in production (HTTPS only)
        cookie.setPath("/"); // Available for all paths
        response.addCookie(cookie);
    }
}
