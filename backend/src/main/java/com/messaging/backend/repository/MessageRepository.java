/*
 * Message Repository Interface
 * 
 * Data Access Layer for Message entity using Spring Data MongoDB.
 * Provides CRUD operations and custom query methods for Message documents in MongoDB.
 * 
 * Spring Data MongoDB automatically implements this interface at runtime,
 * providing standard database operations without writing implementation code.
 * 
 * Custom query methods:
 * - findBySenderIdAndReceiverIdOrReceiverIdAndSenderId: Fetches all messages between two users
 *   This method finds messages where:
 *   (senderId = user1 AND receiverId = user2) OR (senderId = user2 AND receiverId = user1)
 *   Used to display conversation history between two users
 */
package com.messaging.backend.repository;

import com.messaging.backend.entity.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // Marks this as a Spring Data repository component
public interface MessageRepository extends MongoRepository<Message, String> {
    
    /**
     * Find all messages exchanged between two users (bidirectional)
     * Returns messages in chronological order (oldest to newest)
     * 
     * This query finds messages where:
     * - User A sent to User B, OR
     * - User B sent to User A
     * 
     * @param senderId First user's ID
     * @param receiverId Second user's ID
     * @param receiverId2 Second user's ID (for reverse direction)
     * @param senderId2 First user's ID (for reverse direction)
     * @return List of messages between the two users, ordered by creation time
     */
    @Query("{ $or: [ " +
           "{ 'senderId': ?0, 'receiverId': ?1 }, " +
           "{ 'senderId': ?1, 'receiverId': ?0 } " +
           "] }")
    List<Message> findConversationMessages(String senderId, String receiverId);
}
