/*
 * User Entity - MongoDB Document Model
 * 
 * Represents a user in the messaging application and maps to the "users" collection in MongoDB.
 * This entity stores user authentication information and profile data.
 * 
 * Features:
 * - Unique fullname and email for each user
 * - Encrypted password storage
 * - Optional profile picture URL
 * - Automatic timestamp tracking (createdAt, updatedAt)
 * - UserDetails implementation for Spring Security integration
 * 
 * MongoDB Collection: users
 */
package com.messaging.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Data // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: generates no-args constructor
@AllArgsConstructor // Lombok: generates all-args constructor
@Document(collection = "users") // Maps this class to MongoDB "users" collection
public class User implements UserDetails {

    @Id // Marks this field as the primary key (MongoDB _id)
    private String id;

    @Indexed(unique = true) // Creates a unique index on this field in MongoDB
    private String fullname;

    @Indexed(unique = true) // Creates a unique index on this field in MongoDB
    private String email;

    private String password; // Stored as BCrypt hash, never plain text

    private String profilePic; // URL to profile picture (stored in Cloudinary)

    @CreatedDate // Automatically populated with creation timestamp
    private LocalDateTime createdAt;

    @LastModifiedDate // Automatically updated on document modification
    private LocalDateTime updatedAt;

    // ====== UserDetails Interface Implementation for Spring Security ======
    
    /**
     * Returns the authorities/roles granted to the user
     * Currently returns empty collection (can be extended with roles)
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    /**
     * Returns the username used for authentication
     * In this app, email is used as the username
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Indicates whether the user's account has expired
     * Returns true by default (account never expires)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked
     * Returns true by default (account never locked)
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) has expired
     * Returns true by default (credentials never expire)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled
     * Returns true by default (all users are enabled)
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
