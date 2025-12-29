/*
 * Cloudinary Utility Class
 * 
 * This utility class handles image upload operations to Cloudinary service.
 * Cloudinary is a cloud-based image and video management service that provides
 * image upload, storage, transformation, and delivery capabilities.
 * 
 * Features:
 * - Configures Cloudinary with API credentials from application.properties
 * - Uploads base64 encoded images to Cloudinary
 * - Returns secure HTTPS URL of uploaded image
 * - Handles upload errors gracefully
 * 
 * Image upload flow:
 * 1. Receive base64 encoded image string from client
 * 2. Upload to Cloudinary with automatic format detection
 * 3. Get secure URL from upload response
 * 4. Return URL to be stored in database
 * 
 * The uploaded images are accessible via the returned HTTPS URL.
 */
package com.messaging.backend.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component // Marks this as a Spring component (can be autowired)
public class CloudinaryUtil {

    private final Cloudinary cloudinary;

    /**
     * Constructor - initializes Cloudinary with configuration
     * Credentials are injected from application.properties
     * 
     * @param cloudName Cloudinary cloud name
     * @param apiKey Cloudinary API key
     * @param apiSecret Cloudinary API secret
     */
    public CloudinaryUtil(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        // Initialize Cloudinary with credentials
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    /**
     * Uploads an image to Cloudinary
     * Accepts base64 encoded image string (data URI format)
     * Returns secure HTTPS URL of the uploaded image
     * 
     * @param base64Image Base64 encoded image string (e.g., "data:image/png;base64,iVBORw0K...")
     * @return Secure HTTPS URL of uploaded image
     * @throws RuntimeException if upload fails
     */
    public String uploadImage(String base64Image) {
        try {
            // Upload image to Cloudinary
            // Cloudinary automatically detects image format from base64 string
                Map<?, ?> uploadResult = cloudinary.uploader().upload(base64Image, ObjectUtils.emptyMap());
            
            // Extract secure URL from upload result
                String secureUrl = uploadResult.get("secure_url") != null
                    ? uploadResult.get("secure_url").toString()
                    : null;
            
            System.out.println("Image uploaded to Cloudinary: " + secureUrl);
            return secureUrl;
            
        } catch (Exception e) {
            // Log error and throw runtime exception
            System.err.println("Error uploading image to Cloudinary: " + e.getMessage());
            throw new RuntimeException("Failed to upload image to Cloudinary", e);
        }
    }
}
