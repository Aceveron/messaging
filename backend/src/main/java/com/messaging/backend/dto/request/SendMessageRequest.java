/*
 * Send Message Request DTO (Data Transfer Object)
 * 
 * This class represents the request body for sending a message.
 * A message can contain text, an image, or both.
 * 
 * Fields:
 * - text: Message text content (optional)
 * - image: Base64 encoded image string (optional)
 * 
 * At least one field (text or image) should be present.
 * 
 * Used in: POST /api/messages/send/:DmId
 */
package com.messaging.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: generates no-args constructor
@AllArgsConstructor // Lombok: generates all-args constructor
public class SendMessageRequest {
    private String text;  // Message text content (optional)
    private String image; // Base64 encoded image (optional)
}
