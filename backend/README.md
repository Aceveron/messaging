# Messaging Backend - Spring Boot

A high-performance real-time messaging API built with Spring Boot 3.5.0, MongoDB, JWT authentication, and WebSocket (STOMP) for instant message delivery.

## Features

- **User Authentication**: JWT-based login/register with BCrypt password hashing
- **Real-time Messaging**: STOMP over WebSocket for instant message delivery without polling
- **Presence Tracking**: Live online/offline status with automatic detection
- **Profile Management**: Profile picture uploads via Cloudinary
- **Secure by Default**: HTTP-only cookies, CSRF protection, input validation, CORS controls
- **MongoDB Persistence**: Flexible document-based data storage
- **Image Support**: Message images and profile pictures via Cloudinary

## Technology Stack

- **Framework**: Spring Boot 3.5.0
- **Language**: Java 25
- **Build Tool**: Maven
- **Database**: MongoDB
- **Security**: Spring Security + JWT
- **Messaging**: STOMP over WebSocket with Spring Messaging
- **Lombok**: 1.18.30 for annotation-driven code generation
- **Image Storage**: Cloudinary

## Project Structure

```
backend/
├── pom.xml                                 # Maven build configuration
├── README.md                               # This file
├── src/main/
│   ├── resources/
│   │   └── application.properties          # Runtime configuration (DB, JWT, CORS)
│   └── java/com/messaging/backend/
│       ├── MessagingApplication.java       # Spring Boot application entry point
│       ├── config/
│       │   ├── SecurityConfig.java         # Spring Security & JWT filter chain
│       │   └── WebSocketConfig.java        # STOMP broker & WebSocket endpoint config
│       ├── controller/
│       │   ├── AuthController.java         # Login, register, profile, auth status
│       │   ├── MessageController.java      # Message CRUD & conversation retrieval
│       │   └── PresenceController.java     # Online users status endpoint
│       ├── dto/
│       │   ├── request/                    # Request payload models
│       │   │   ├── LoginRequest.java
│       │   │   ├── RegisterRequest.java
│       │   │   ├── SendMessageRequest.java
│       │   │   └── UpdateProfileRequest.java
│       │   └── response/                   # Response payload models
│       │       ├── AuthResponse.java
│       │       ├── MessageResponse.java
│       │       └── UserResponse.java
│       ├── entity/
│       │   ├── Message.java                # MongoDB message document
│       │   └── User.java                   # MongoDB user document
│       ├── repository/
│       │   ├── MessageRepository.java      # Message data queries
│       │   └── UserRepository.java         # User data queries
│       ├── security/
│       │   └── JwtAuthenticationFilter.java # Token extraction & validation
│       ├── service/
│       │   ├── AuthService.java            # Auth business logic
│       │   ├── CustomUserDetailsService.java # Spring Security user loader
│       │   └── MessageService.java         # Message persistence logic
│       ├── util/
│       │   ├── CloudinaryUtil.java         # Image upload integration
│       │   └── JwtUtil.java                # Token generation & parsing
│       └── websocket/
│           ├── WebSocketHandler.java       # STOMP message routing & presence
│           ├── WebSocketPresenceListener.java # Connection/disconnect events
│           └── UserHandshakeInterceptor.java # Extract userId from handshake
```

## API Endpoints

### Authentication (`/api/auth`)
- `POST /login` – Login with email/password; returns JWT in HTTP-only cookie
- `POST /register` – Register new user with email/password validation
- `POST /logout` – Clear JWT cookie and session
- `PUT /profile` – Update profile picture (authenticated)
- `GET /pulse` – Check authentication status and fetch current user (authenticated)

### Messages (`/api/messages`)
- `GET /users` – List all users for sidebar (authenticated)
- `GET /{userId}` – Fetch message history with a specific user (authenticated)
- `POST /send/{userId}` – Send message (text and/or image) to user (authenticated)

### Presence (`/api/presence`)
- `GET /online` – Fetch list of currently online user IDs

### WebSocket (Real-time)
- **Connect**: `ws://localhost:5001/ws?userId={userId}`
  - Opens a persistent STOMP connection authenticated via JWT cookie
  - Server broadcasts online user list on connect/disconnect

- **Subscribe to receive messages**: `/user/topic/messages`
  - Receives messages sent to the authenticated user
  - Routed via `convertAndSendToUser()` on the server

- **Subscribe to online status**: `/topic/onlineUsers`
  - Broadcasts updated list of online user IDs whenever presence changes
  - All connected clients receive updates

## Configuration

Create or update `src/main/resources/application.properties`:

```properties
# Server
server.port=5001

# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/messaging

# JWT
jwt.secret=your-256-bit-secret-key-at-least-32-characters-long
jwt.expiration=86400000

# Cloudinary (for image uploads)
cloudinary.cloud-name=your-cloud-name
cloudinary.api-key=your-api-key
cloudinary.api-secret=your-api-secret

# CORS (match your frontend URL)
cors.allowed-origins=http://localhost:5173,http://localhost:3000
```

**Notes**:
- `jwt.secret` should be at least 256 bits (32 characters) in production
- Update `cors.allowed-origins` to match your frontend domain
- Cloudinary is optional; omit credentials to disable image uploads

## Setup and Installation

### Prerequisites
- **Java 25** or higher
- **Maven 3.8+**
- **MongoDB 5.0+** (local or Atlas URI)
- **Cloudinary account** (optional, for image uploads)

### Installation Steps

1. **Navigate to backend directory**
   ```bash
   cd backend
   ```

2. **Configure environment**
   - Copy or create `src/main/resources/application.properties`
   - Fill in MongoDB URI, JWT secret, Cloudinary credentials, CORS origins

3. **Build and install dependencies**
   ```bash
   mvn clean install
   ```

4. **Run in development mode** (with hot reload)
   ```bash
   mvn spring-boot:run
   ```

5. **Or build and run the executable JAR**
   ```bash
   mvn clean package
   java -jar target/backend-1.0.0.jar
   ```

6. **Verify startup**
   - Server runs on `http://localhost:5001`
   - Check logs for "Started MessagingApplication"
   - Ensure MongoDB is accessible

### Development Tips
- Spring Boot DevTools enables hot reload; changes take effect immediately
- Set `logging.level.com.messaging.backend=DEBUG` in properties for detailed logs
- Use `mvn clean package -DskipTests` for faster builds without running tests

## Architecture

### Authentication Flow
1. Client POSTs credentials to `/api/auth/login`
2. Server validates and returns JWT in HTTP-only, SameSite cookie
3. JWT filter (`JwtAuthenticationFilter`) extracts token from cookie on each request
4. Spring Security context is populated for endpoint access control

### Real-time Messaging Flow
1. WebSocket client connects to `/ws` with `userId` query parameter
2. Server validates JWT from cookie and establishes STOMP connection
3. Client subscribes to `/user/topic/messages` to listen for incoming messages
4. Sender POSTs to `/api/messages/send/{userId}`
5. Server checks receiver is online via `SimpUserRegistry`
6. If online, server sends message via `convertAndSendToUser()` → `/user/{receiverId}/topic/messages`
7. Client's STOMP subscription callback receives the message and updates state

### Presence Tracking
- On WebSocket connect: server adds user to `userSocketMap`, broadcasts updated online list
- On disconnect: server removes user from map, broadcasts updated online list
- Clients subscribe to `/topic/onlineUsers` to receive live presence updates

## Security

- **Password Hashing**: BCrypt with automatic salt generation
- **JWT Tokens**: Stateless, expires in 24 hours by default (configurable)
- **HTTP-only Cookies**: Token stored securely; inaccessible to JavaScript (XSS protection)
- **SameSite Cookies**: CSRF protection via `SameSite=Strict` attribute
- **CORS**: Restricted to allowed origins only; prevents unauthorized cross-origin requests
- **Input Validation**: Request DTOs with `@Validated` annotations
- **Null Safety**: Spring Boot 3.5.0 with improved null-safety checks via Lombok

## Database Schema

### User Collection
```json
{
  "_id": "ObjectId",
  "fullname": "String (unique)",
  "email": "String (unique, lowercase)",
  "password": "String (BCrypt hashed)",
  "profilePic": "String (Cloudinary URL, optional)",
  "createdAt": "DateTime (ISO 8601)",
  "updatedAt": "DateTime (ISO 8601)"
}
```

### Message Collection
```json
{
  "_id": "ObjectId",
  "text": "String (optional)",
  "image": "String (Cloudinary URL, optional)",
  "senderId": "String (User ObjectId)",
  "receiverId": "String (User ObjectId)",
  "createdAt": "DateTime (ISO 8601)",
  "updatedAt": "DateTime (ISO 8601)"
}
```

## Testing

Run the test suite:
```bash
mvn test
```

Skip tests during build (for faster CI/CD):
```bash
mvn clean package -DskipTests
```

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `MongoException: connection refused` | MongoDB not running | Start MongoDB: `mongod` or connect to Atlas URI |
| `JWT validation failed` | Invalid or expired token | Re-login to get a new JWT token |
| `WebSocket connection failed` | Invalid JWT or CORS origin not allowed | Check CORS config and JWT cookie in browser dev tools |
| `Message not received in real-time` | Receiver not online | Ensure both users are connected to WebSocket |
| `Image upload fails` | Invalid Cloudinary credentials | Verify API key/secret in application.properties |

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

Licensed under ISC.

## Support

For issues, questions, or feature requests, open an issue on GitHub or contact the author.

---

**Last Updated**: December 2025  
**Java**: 25  
**Spring Boot**: 3.5.0  
**Lombok**: 1.18.30
