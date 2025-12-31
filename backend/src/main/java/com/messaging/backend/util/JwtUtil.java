/*
 * JWT Utility Class
 * 
 * This utility class handles JWT (JSON Web Token) operations including:
 * - Token generation for authenticated users
 * - Token validation and verification
 * - Extracting user information from tokens
 * 
 * JWT tokens are used for stateless authentication, allowing the server to verify
 * user identity without maintaining session state. Tokens are signed with a secret key
 * and have an expiration time for security.
 * 
 * The token contains the user's email as the subject and is valid for 24 hours by default.
 */
package com.messaging.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component // Marks this as a Spring component (can be autowired)
public class JwtUtil {

    @Value("${jwt.secret}") // Injects JWT secret from application.properties
    private String secret;

    @Value("${jwt.expiration}") // Injects JWT expiration time from application.properties
    private Long expiration;

    /**
     * Generates a signing key from the secret string
     * Converts the secret string to bytes and creates an HMAC-SHA key
     * 
     * @return SecretKey for signing and verifying JWT tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT token for a user
     * Token contains:
     * - Subject: user's email
     * - Issued at: current timestamp
     * - Expiration: current time + expiration period
     * - Signature: signed with secret key
     * 
     * @param email User's email address (used as token subject)
     * @return JWT token string
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email) // Set user email as subject
                .issuedAt(new Date()) // Set issue timestamp
                .expiration(new Date(System.currentTimeMillis() + expiration)) // Set expiration
                .signWith(getSigningKey(), Jwts.SIG.HS256) // Sign with secret key
                .compact(); // Build and return token string
    }

    /**
     * Extracts the email (subject) from a JWT token
     * 
     * @param token JWT token string
     * @return Email address extracted from token
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts all claims from a JWT token
     * Claims contain the token payload (subject, expiration, etc.)
     * 
     * @param token JWT token string
     * @return Claims object containing all token data
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Set signing key for verification
                .build()
                .parseSignedClaims(token) // Parse and verify token
                .getPayload(); // Return claims
    }

    /**
     * Validates a JWT token
     * Checks if:
     * - Token email matches the provided email
     * - Token is not expired
     * 
     * @param token JWT token string
     * @param email Email to validate against
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token, String email) {
        try {
            String tokenEmail = extractEmail(token);
            return tokenEmail.equals(email) && !isTokenExpired(token);
        } catch (Exception e) {
            return false; // Invalid token format or signature
        }
    }

    /**
     * Checks if a token is expired
     * 
     * @param token JWT token string
     * @return true if token is expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Generates a short-lived JWT token for media download (30 seconds)
     * Token contains:
     * - mediaId: ID of media file
     * - userId: ID of user requesting access
     * - Expiration: 30 seconds
     * 
     * @param mediaId Media file ID
     * @param userId User ID
     * @return Short-lived JWT token
     */
    public String generateMediaToken(String mediaId, String userId) {
        return Jwts.builder()
                .subject(userId) // User ID
                .claim("mediaId", mediaId) // Media ID
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 30000)) // 30 seconds
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validates a media download token
     * Checks if:
     * - Token userId matches provided userId
     * - Token mediaId matches provided mediaId
     * - Token is not expired
     * 
     * @param token Media token string
     * @param mediaId Expected media ID
     * @param userId Expected user ID
     * @return true if token is valid, false otherwise
     */
    public boolean validateMediaToken(String token, String mediaId, String userId) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenUserId = claims.getSubject();
            String tokenMediaId = claims.get("mediaId", String.class);
            
            return tokenUserId.equals(userId) 
                && tokenMediaId.equals(mediaId) 
                && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
