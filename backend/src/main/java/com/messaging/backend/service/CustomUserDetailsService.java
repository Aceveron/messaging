/*
 * Custom User Details Service
 * 
 * This service implements Spring Security's UserDetailsService interface.
 * It's responsible for loading user details from the database during authentication.
 * 
 * Spring Security uses this service to:
 * - Load user by username (email in our case) during login
 * - Verify user credentials
 * - Populate the SecurityContext with user information
 * 
 * The User entity implements UserDetails interface, so it can be returned directly.
 */
package com.messaging.backend.service;

import com.messaging.backend.entity.User;
import com.messaging.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service // Marks this as a Spring service component
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository; // Repository for database operations

    /**
     * Loads user details by username (email)
     * Called by Spring Security during authentication
     * 
     * @param email User's email address (used as username)
     * @return UserDetails object containing user information
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Find user by email in database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        // Return user (User entity implements UserDetails)
        return user;
    }
}
