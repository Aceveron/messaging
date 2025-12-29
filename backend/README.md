# Messaging Backend - Spring Boot

A real-time messaging application backend built with Spring Boot, MongoDB, JWT authentication, and WebSocket support.

## Features

- **User Authentication**: Register, login, logout with JWT token-based authentication
- **Real-time Messaging**: WebSocket-based real-time message delivery
- **Profile Management**: Update profile pictures via Cloudinary
- **Secure**: BCrypt password hashing, HTTP-only cookies, CORS protection
- **Database**: MongoDB for data persistence
- **Image Upload**: Cloudinary integration for profile pictures and message images

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: MongoDB
- **Authentication**: JWT (JSON Web Tokens)
- **Security**: Spring Security
- **Real-time**: WebSocket with STOMP protocol
- **Image Storage**: Cloudinary
- **Build Tool**: Maven

## Project Structure

```
backend-java/
├── pom.xml                                 # Maven configuration
├── main/
│   ├── resources/
│   │   └── application.properties          # Application configuration
│   └── java/com/messaging/backend/
│       ├── MessagingApplication.java       # Main application entry point
│       ├── config/
│       │   ├── SecurityConfig.java         # Spring Security configuration
│       │   └── WebSocketConfig.java        # WebSocket configuration
│       ├── controller/
│       │   ├── AuthController.java         # Authentication endpoints
│       │   └── MessageController.java      # Messaging endpoints
│       ├── dto/
│       │   ├── request/                    # Request DTOs
│       │   │   ├── LoginRequest.java
│       │   │   ├── RegisterRequest.java
│       │   │   ├── SendMessageRequest.java
│       │   │   └── UpdateProfileRequest.java
│       │   └── response/                   # Response DTOs
│       │       ├── AuthResponse.java
│       │       ├── ErrorResponse.java
│       │       ├── MessageResponse.java
│       │       └── UserResponse.java
│       ├── entity/
│       │   ├── Message.java                # Message entity (MongoDB document)
│       │   └── User.java                   # User entity (MongoDB document)
│       ├── repository/
│       │   ├── MessageRepository.java      # Message data access
│       │   └── UserRepository.java         # User data access
│       ├── security/
│       │   └── JwtAuthenticationFilter.java # JWT filter
│       ├── service/
│       │   ├── AuthService.java            # Authentication business logic
│       │   ├── CustomUserDetailsService.java # User loading for Spring Security
│       │   └── MessageService.java         # Messaging business logic
│       ├── util/
│       │   ├── CloudinaryUtil.java         # Cloudinary integration
│       │   └── JwtUtil.java                # JWT utilities
│       └── websocket/
│           └── WebSocketHandler.java       # WebSocket handler
```

## API Endpoints

### Authentication (`/api/auth`)

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `POST /api/auth/logout` - Logout user
- `PUT /api/auth/profile` - Update profile picture (authenticated)
- `GET /api/auth/pulse` - Check authentication status (authenticated)

### Messages (`/api/messages`)

- `GET /api/messages/users` - Get all users for sidebar (authenticated)
- `GET /api/messages/:DmId` - Get conversation messages (authenticated)
- `POST /api/messages/send/:DmId` - Send message (authenticated)

### WebSocket

- Connect to: `ws://localhost:5001/ws?userId={userId}`
- Subscribe to: `/user/{userId}/topic/messages` - Receive real-time messages
- Subscribe to: `/topic/onlineUsers` - Receive online users updates

## Configuration

Create `main/resources/application.properties` with the following variables:

```properties
# Server
server.port=5001

# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/messaging

# JWT
jwt.secret=your-secret-key-at-least-256-bits-long
jwt.expiration=86400000

# Cloudinary
cloudinary.cloud-name=your-cloud-name
cloudinary.api-key=your-api-key
cloudinary.api-secret=your-api-secret

# CORS
cors.allowed-origins=http://localhost:5173
```

## Setup and Installation

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MongoDB (running locally or remote connection)
- Cloudinary account (for image uploads)

### Installation Steps

1. **Clone or navigate to the project directory**
   ```bash
   cd backend
   ```

2. **Configure environment variables**
   - Update `main/resources/application.properties` with your MongoDB URI, JWT secret, and Cloudinary credentials

3. **Install dependencies**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   Or build and run the JAR:
   ```bash
   mvn clean package
   java -jar target/backend-1.0.0.jar
   ```

5. **The server will start on port 5001**
   ```
   http://localhost:5001
   ```

## Development

### Running in Development Mode

```bash
mvn spring-boot:run
```

Spring Boot DevTools is included for hot reload during development.

### Building for Production

```bash
mvn clean package -DskipTests
```

The executable JAR will be created in `target/backend-1.0.0.jar`

## Security Features

- **Password Hashing**: BCrypt with salt
- **JWT Tokens**: Stateless authentication
- **HTTP-only Cookies**: Prevents XSS attacks
- **CORS Configuration**: Controlled cross-origin access
- **CSRF Protection**: SameSite cookie attribute
- **Input Validation**: Request data validation

## Database Schema

### User Collection
```json
{
  "_id": "ObjectId",
  "fullname": "String (unique)",
  "email": "String (unique)",
  "password": "String (BCrypt hashed)",
  "profilePic": "String (URL)",
  "createdAt": "DateTime",
  "updatedAt": "DateTime"
}
```

### Message Collection
```json
{
  "_id": "ObjectId",
  "text": "String (optional)",
  "senderId": "String (User ID)",
  "receiverId": "String (User ID)",
  "image": "String (URL, optional)",
  "createdAt": "DateTime",
  "updatedAt": "DateTime"
}
```

## Testing

Run tests with:
```bash
mvn test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the ISC License.

## Author

Aceveronn

## Notes

- Make sure MongoDB is running before starting the application
- JWT secret should be at least 256 bits (32 characters) for production
- Update CORS origins in application.properties to match your frontend URL
- Cloudinary credentials are required for profile picture and image message features
