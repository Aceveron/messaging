/*
 * Security Configuration Class
 * 
 * This class configures Spring Security for the messaging application.
 * It defines authentication mechanisms, authorization rules, and security filters.
 * 
 * Key configurations:
 * Stateless session management (using JWT, no server-side sessions)
 * CORS enabled for frontend communication
 * CSRF disabled (not needed for stateless JWT authentication)
 * Public endpoints: /api/auth/register, /api/auth/login
 * Protected endpoints: Everything else requires authentication
 * Custom JWT filter added before UsernamePasswordAuthenticationFilter
 * BCrypt password encoding for secure password storage
 * 
 * Security flow:
 * 1. Request arrives
 * 2. CORS filter processes it
 * 3. JWT filter validates token and sets authentication
 * 4. Authorization rules check if user has access
 * 5. Request proceeds to controller or returns 401/403 error
 */
package com.messaging.backend.config;

import com.messaging.backend.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration // Marks this as a configuration class
@EnableWebSecurity // Enables Spring Security
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter; // Custom JWT filter

    @Autowired
    private UserDetailsService userDetailsService; // Service to load user details

    @Value("${cors.allowed-origins}") // Injects allowed origins from application.properties
    private String allowedOrigins;

    /**
     * Configures the security filter chain
     * Defines authentication and authorization rules
     * 
     * @param http HttpSecurity object to configure
     * @return Configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (not needed for stateless JWT authentication)
                .csrf(csrf -> csrf.disable())
                
                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                    .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                    // Allow auth pulse endpoint for session checks
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/auth/pulse").permitAll()
                        // WebSocket endpoint (authentication handled separately)
                        .requestMatchers("/ws/**").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                
                // Stateless session management (no server-side sessions)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing)
     * Allows frontend application to make requests to this backend
     * 
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins from application.properties
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        
        // Allow all HTTP methods (GET, POST, PUT, DELETE, etc.)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Creates a password encoder bean
     * Uses BCrypt algorithm for secure password hashing
     * 
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt with default strength (10)
    }

    /**
     * Creates an authentication provider
     * Uses UserDetailsService and PasswordEncoder for authentication
     * 
     * @return DaoAuthenticationProvider instance
     */
    @Bean
    @SuppressWarnings("deprecation")
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes AuthenticationManager as a bean
     * Used for manual authentication (e.g., during login)
     * 
     * @param config Authentication configuration
     * @return AuthenticationManager instance
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
