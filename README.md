# Enterprise Procurement System (EPS)

A comprehensive enterprise procurement management system built with **Java 21**, **Spring Boot 3.x**, **Spring Security**, and **MySQL**.

## Project Overview

The Enterprise Procurement System is a role-based web application that provides:
- User and Admin registration with BCrypt password encryption
- Role-based access control (ADMIN and USER roles)
- REST APIs for authentication and dashboard management
- Bootstrap-based responsive UI
- Comprehensive exception handling
- Unit tests with TestNG

## Technology Stack

- **Java Version**: Java 21
- **Spring Boot**: 3.3.1
- **Spring Security**: OAuth2 with JWT support
- **Database**: MySQL 8.0+
- **Build Tool**: Maven
- **Testing Framework**: TestNG
- **Frontend**: HTML5, Bootstrap 5, JavaScript
- **ORM**: Hibernate/JPA
- **Additional Libraries**: 
  - Lombok (for reducing boilerplate code)
  - ModelMapper (for DTO mapping)
  - Jackson (for JSON processing)

## Project Structure

```
enterprise/
├── src/
│   ├── main/
│   │   ├── java/com/eps/
│   │   │   ├── config/              # Spring Configuration classes
│   │   │   ├── controller/          # REST Controllers
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── entity/              # JPA Entities
│   │   │   ├── exception/           # Custom Exceptions & Global Exception Handler
│   │   │   ├── repository/          # JPA Repositories
│   │   │   ├── security/            # Security-related classes
│   │   │   ├── service/             # Service Interfaces
│   │   │   ├── service/impl/        # Service Implementations
│   │   │   └── util/                # Utility Classes
│   │   └── resources/
│   │       ├── static/              # HTML pages, CSS, JavaScript
│   │       └── application.properties # Application configuration
│   └── test/
│       ├── java/com/eps/            # Unit Tests
│       └── resources/testng.xml     # TestNG Configuration
├── pom.xml                          # Maven configuration
└── README.md                        # This file
```

## Database Setup

### Prerequisites
- MySQL 8.0 or higher installed and running
- MySQL command-line client or MySQL Workbench

### Steps to Setup Database

1. **Open MySQL Command Line or MySQL Workbench**

2. **Create the Database**
```sql
CREATE DATABASE enterprise_procurement;
```

3. **Verify Database Creation**
```sql
SHOW DATABASES;
```

The tables will be created automatically when you run the Spring Boot application (thanks to `spring.jpa.hibernate.ddl-auto=update` in application.properties).

## Configuration Setup

### 1. Update application.properties

Navigate to `src/main/resources/application.properties` and update the MySQL connection details:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/enterprise_procurement
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

Replace `YOUR_PASSWORD` with your MySQL root password.

### 2. Verify Other Configuration

The following settings are pre-configured:
- Server Port: `8080`
- JPA/Hibernate: Auto-update mode enabled
- Logging: DEBUG level for com.eps package

## Building and Running the Application

### Prerequisites
- Java 21 JDK installed
- Maven installed
- MySQL database running

### Steps to Run

1. **Open Terminal/Command Prompt** in the project directory

2. **Build the Project**
```bash
mvn clean install
```

3. **Run the Application**
```bash
mvn spring-boot:run
```

Or run as JAR:
```bash
mvn clean package
java -jar target/enterprise-procurement-system-1.0.0.jar
```

4. **Verify Application is Running**
```
Server should start on http://localhost:8080
Check logs for: "Started EnterpriseProcurementSystemApplication"
```

## Initial Data Setup

When the application starts for the first time, you need to create initial roles:

1. **Access the database directly**
```sql
USE enterprise_procurement;

INSERT INTO role (role_name) VALUES ('ADMIN');
INSERT INTO role (role_name) VALUES ('USER');
```

Or use a database client to insert these two roles in the `role` table.

## API Endpoints

### Authentication APIs

#### 1. Register User
```
POST /api/auth/register
Content-Type: application/json

{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "Password123",
  "phone": "1234567890",
  "address": "123 Main Street"
}

Response:
{
  "success": true,
  "message": "User registered successfully"
}
```

#### 2. Register Admin (Only ADMIN can create another ADMIN)
```
POST /api/auth/admin/register
Content-Type: application/json

{
  "fullName": "Admin User",
  "email": "admin@example.com",
  "password": "Password123",
  "phone": "9876543210",
  "address": "Admin Address"
}

Response:
{
  "success": true,
  "message": "Admin registered successfully"
}
```

#### 3. Login
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "Password123"
}

Response:
{
  "success": true,
  "message": "Login successful",
  "data": {
    "userId": 1,
    "fullName": "John Doe",
    "email": "john@example.com",
    "role": "USER",
    "success": true
  }
}
```

### Dashboard APIs

#### 4. Admin Dashboard (Admin Only)
```
GET /admin/dashboard

Response:
{
  "success": true,
  "message": "Admin Dashboard loaded successfully",
  "data": {
    "dashboardName": "Admin Dashboard",
    "totalUsers": 0,
    "totalAdmins": 0,
    "systemStatus": "Active",
    "lastUpdated": 1234567890
  }
}
```

#### 5. User Dashboard (User Only)
```
GET /user/dashboard

Response:
{
  "success": true,
  "message": "User Dashboard loaded successfully",
  "data": {
    "dashboardName": "User Dashboard",
    "recentActivity": "No recent activity",
    "accountStatus": "Active",
    "lastLogin": 1234567890
  }
}
```

## Frontend Pages

### 1. Login Page
**URL**: `http://localhost:8080/login.html`

Features:
- User and Admin login tabs
- Email and password input fields
- Automatic redirection based on role
- Bootstrap styling

### 2. Register Page
**URL**: `http://localhost:8080/register.html`

Features:
- User registration form
- Full name, email, password, phone, address fields
- Password validation rules display
- Automatic redirection to login after successful registration

### 3. Admin Dashboard
**URL**: `http://localhost:8080/admin-dashboard.html` (after login as ADMIN)

Features:
- Admin navigation sidebar
- System statistics cards
- System information display
- Manage Users, Reports, Settings options

### 4. User Dashboard
**URL**: `http://localhost:8080/user-dashboard.html` (after login as USER)

Features:
- User navigation sidebar
- User profile information
- Dashboard overview
- Recent activity section

## Testing with Postman

### Setting Up Postman

1. **Download and Install Postman** from https://www.postman.com/downloads/

2. **Import Postman Collection**
   - In Postman, click `Import`
   - Copy the collection JSON from `postman/Enterprise-Procurement-System.postman_collection.json`
   - The collection includes all API endpoints pre-configured

### Manual API Testing

#### Test Case 1: Register User
1. Create a new POST request
2. URL: `http://localhost:8080/api/auth/register`
3. Set Headers: `Content-Type: application/json`
4. Body (JSON):
```json
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "Password123",
  "phone": "1234567890",
  "address": "123 Main Street"
}
```
5. Click Send
6. Expected: 201 Created with success message

#### Test Case 2: Login User
1. Create a new POST request
2. URL: `http://localhost:8080/api/auth/login`
3. Set Headers: `Content-Type: application/json`
4. Body (JSON):
```json
{
  "email": "john@example.com",
  "password": "Password123"
}
```
5. Click Send
6. Expected: 200 OK with user data and role

#### Test Case 3: Access User Dashboard
1. Create a new GET request
2. URL: `http://localhost:8080/user/dashboard`
3. Click Send
4. Expected: 200 OK with dashboard data

#### Test Case 4: Access Admin Dashboard
1. Create a new GET request
2. URL: `http://localhost:8080/admin/dashboard`
3. Click Send
4. Expected: 200 OK with admin dashboard data (only accessible by ADMIN)

## Running Unit Tests

### Using Maven
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthenticationServiceTest

# Run with TestNG
mvn test -Dsuites=src/test/resources/testng.xml
```

### Using IDE
- Right-click on test class or test method
- Select "Run As" → "JUnit Test" or "TestNG Test"

## Password Requirements

- Minimum 8 characters
- Must contain at least one uppercase letter (A-Z)
- Must contain at least one lowercase letter (a-z)
- Must contain at least one number (0-9)

Example valid passwords:
- `Password123`
- `MySecurePass456`
- `Test@Password789`

## Exception Handling

The application includes a Global Exception Handler that returns consistent error responses:

### Error Response Format
```json
{
  "success": false,
  "message": "Error message",
  "error": "ERROR_CODE",
  "data": null
}
```

### Handled Exceptions
- **UserNotFoundException**: User not found in database
- **EmailAlreadyExistsException**: Email already registered
- **InvalidPasswordException**: Wrong password provided
- **MethodArgumentNotValidException**: Validation errors on input fields
- **Generic Exception**: Any other unexpected errors

## Security Features

1. **BCrypt Password Encoding**: All passwords are encrypted using BCrypt
2. **Role-Based Access Control**: Endpoints protected with @PreAuthorize annotations
3. **Input Validation**: All DTOs have validation annotations (@Valid, @NotBlank, @Email)
4. **CORS Configuration**: Cross-origin requests are handled securely
5. **Spring Security**: Integrated with Spring Security for authentication

## Logging Configuration

Logging levels are set in `application.properties`:

```properties
# Root level
logging.level.root=INFO

# Application specific (EPS)
logging.level.com.eps=DEBUG

# Spring Security
logging.level.org.springframework.security=DEBUG

# Hibernate SQL
logging.level.org.hibernate.SQL=DEBUG
```

View logs in:
- IDE console
- Log files (configured location)

## Troubleshooting

### Database Connection Issues
```
Error: "Access denied for user 'root'@'localhost'"
Solution: Check password in application.properties
```

### Port Already in Use
```
Error: "Port 8080 already in use"
Solution: Change port in application.properties:
server.port=8081
```

### Missing Roles in Database
```
Error: "ADMIN role not found"
Solution: Insert roles manually using SQL:
INSERT INTO role (role_name) VALUES ('ADMIN');
INSERT INTO role (role_name) VALUES ('USER');
```

### Maven Build Fails
```
Solution: 
1. mvn clean
2. Delete .m2 repository cache
3. mvn install
```

## Additional Configuration

### Enable Additional Logging
In `application.properties`:
```properties
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG
```

### Change Server Port
In `application.properties`:
```properties
server.port=9090
```

### Disable Hibernate SQL Logging
In `application.properties`:
```properties
spring.jpa.show-sql=false
```

## Database Schema

### Role Table
```sql
CREATE TABLE role (
  role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_name VARCHAR(50) NOT NULL UNIQUE
);
```

### User Table
```sql
CREATE TABLE user (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(20),
  address TEXT,
  enabled BOOLEAN DEFAULT TRUE,
  created_date TIMESTAMP NOT NULL,
  updated_date TIMESTAMP,
  role_id BIGINT NOT NULL,
  FOREIGN KEY (role_id) REFERENCES role(role_id)
);
```

## Future Enhancements

- [ ] JWT Token-based authentication
- [ ] Refresh token mechanism
- [ ] Email verification
- [ ] Two-factor authentication
- [ ] Procurement request management
- [ ] Vendor management
- [ ] Purchase order management
- [ ] Invoice tracking
- [ ] Approval workflows
- [ ] Reporting and analytics

## Support

For issues or questions:
1. Check the logs for error messages
2. Verify database connectivity
3. Ensure MySQL is running
4. Check application.properties configuration
5. Review test cases for usage examples

## License

This project is open source and available under the MIT License.

---

**Version**: 1.0.0  
**Last Updated**: 2026-07-27  
**Developer**: Enterprise Development Team
