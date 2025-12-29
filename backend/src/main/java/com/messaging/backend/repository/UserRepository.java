/*
 * User Repository Interface
 * 
 * Data Access Layer for User entity using Spring Data MongoDB.
 * Provides CRUD operations and custom query methods for User documents in MongoDB.
 * 
 * Spring Data MongoDB automatically implements this interface at runtime,
 * providing standard database operations without writing implementation code.
 * 
 * Custom query methods:
 * - findByEmail: Find user by email address (used for login)
 * - findByFullname: Find user by fullname (used for username uniqueness check)
 * - existsByEmail: Check if email already exists (used during registration)
 * - existsByFullname: Check if fullname already exists (used during registration)
 */
package com.messaging.backend.repository;

import com.messaging.backend.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Marks this as a Spring Data repository component
public interface UserRepository extends MongoRepository<User, String> {
    
    /**
     * Find a user by their email address
     * Used during login authentication
     * 
     * @param email User's email address
     * @return Optional containing the User if found, empty otherwise
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find a user by their fullname
     * Used to check username uniqueness during registration
     * 
     * @param fullname User's fullname
     * @return Optional containing the User if found, empty otherwise
     */
    Optional<User> findByFullname(String fullname);
    
    /**
     * Check if an email already exists in the database
     * Used for email uniqueness validation during registration
     * 
     * @param email Email address to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if a fullname already exists in the database
     * Used for username uniqueness validation during registration
     * 
     * @param fullname Fullname to check
     * @return true if fullname exists, false otherwise
     */
    boolean existsByFullname(String fullname);
}
