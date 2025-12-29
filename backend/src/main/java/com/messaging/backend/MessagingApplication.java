/*
 * Main Application Entry Point for Spring Boot Messaging Application
 * 
 * This is the main class that bootstraps the Spring Boot application.
 * It initializes the Spring context, auto-configures components, and starts the embedded server.
 * 
 * @SpringBootApplication annotation enables:
 * - @Configuration: Marks this as a source of bean definitions
 * - @EnableAutoConfiguration: Enables Spring Boot's auto-configuration mechanism
 * - @ComponentScan: Scans for Spring components in the current package and sub-packages
 * 
 * The application provides:
 * - RESTful APIs for authentication and messaging
 * - WebSocket support for real-time messaging
 * - JWT-based authentication
 * - MongoDB database integration
 * - Cloudinary integration for image uploads
 */
package com.messaging.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MessagingApplication {

    /**
     * Main method - application entry point
     * Starts the Spring Boot application by launching the embedded web server
     * and initializing all configured beans and components
     * 
    @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MessagingApplication.class, args);
        System.out.println("===========================================");
        System.out.println("Messaging Backend Application Started");
        System.out.println("===========================================");
    }
}
