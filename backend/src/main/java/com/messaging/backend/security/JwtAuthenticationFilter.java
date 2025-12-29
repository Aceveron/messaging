/*
 * JWT Authentication Filter
 * 
 * This filter intercepts every HTTP request to check for JWT authentication.
 * It runs before Spring Security's authentication process and validates JWT tokens.
 * 
 * Flow:
 * 1. Extract JWT token from "token" cookie
 * 2. If token exists, validate it and extract user email
 * 3. Load user details from database
 * 4. Create authentication object and set it in SecurityContext
 * 5. Continue with the filter chain
 * 
 * This filter enables stateless authentication - the server doesn't maintain session state.
 * User identity is verified on every request using the JWT token.
 */
package com.messaging.backend.security;

import com.messaging.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // Marks this as a Spring component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil; // JWT utility for token operations

    @Autowired
    private UserDetailsService userDetailsService; // Service to load user details

    /**
     * Filter method that processes every HTTP request
     * Extracts and validates JWT token, then authenticates the user
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Filter chain to continue processing
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // Extract JWT token from cookies
        String token = extractTokenFromCookies(request);
        
        // If token exists and no authentication is already set
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Extract email from token
                String email = jwtUtil.extractEmail(token);
                
                // Load user details from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                
                // Validate token against user details
                if (jwtUtil.validateToken(token, email)) {
                    // Create authentication object
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    
                    // Set additional details
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    // Set authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // Log error but don't block the request
                System.err.println("JWT Authentication error: " + e.getMessage());
            }
        }
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from request cookies
     * Looks for a cookie named "token"
     * 
     * @param request HTTP request containing cookies
     * @return JWT token string, or null if not found
     */
    private String extractTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
