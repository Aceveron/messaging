/*
 * Media Repository
 * 
 * MongoDB repository for Media entity operations.
 * Provides database access for encrypted media metadata.
 */
package com.messaging.backend.repository;

import com.messaging.backend.entity.Media;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends MongoRepository<Media, String> {

    /**
     * Find media by mediaId (UUID)
     */
    Optional<Media> findByMediaId(String mediaId);

    /**
     * Find all media uploaded by a specific user
     */
    List<Media> findByUploaderId(String uploaderId);

    /**
     * Find expired media for cleanup job
     */
    List<Media> findByExpiresAtBefore(LocalDateTime dateTime);

    /**
     * Delete media by mediaId
     */
    void deleteByMediaId(String mediaId);
}
