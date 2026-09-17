# Enterprise Procurement System - Setup & Installation Guide

## Complete Step-by-Step Guide

This guide provides detailed instructions to set up and run the Enterprise Procurement System (EPS) locally.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [MySQL Database Setup](#mysql-database-setup)
3. [Project Configuration](#project-configuration)
4. [Building the Application](#building-the-application)
5. [Running the Application](#running-the-application)
6. [Initial Data Setup](#initial-data-setup)
7. [Postman API Testing](#postman-api-testing)
8. [Frontend Testing](#frontend-testing)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### System Requirements
- **Operating System**: Windows, macOS, or Linux
- **RAM**: Minimum 4GB (8GB recommended)
- **Disk Space**: Minimum 2GB free space

### Required Software

#### 1. Java Development Kit (JDK) 21
- Download from: https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html
- Or use OpenJDK: https://jdk.java.net/21/

**Verify Installation:**
```bash
java -version
javac -version
```

#### 2. Apache Maven 3.8.0 or Higher
- Download from: https://maven.apache.org/download.cgi
- Extract to a preferred location

**Verify Installation:**
```bash
mvn --version
```

#### 3. MySQL Server 8.0+
- Download Community Edition: https://dev.mysql.com/downloads/mysql/
- During installation, note the root password

**Verify Installation:**
```bash
mysql --version
```

#### 4. Git (Optional)
- Download from: https://git-scm.com/downloads

#### 5. Postman (For API Testing)
- Download from: https://www.postman.com/downloads/

#### 6. IDE (Recommended)
- IntelliJ IDEA Community: https://www.jetbrains.com/idea/download/
- Visual Studio Code: https://code.visualstudio.com/
- Eclipse: https://www.eclipse.org/downloads/

---

## MySQL Database Setup

### Step 1: Start MySQL Server

#### Windows:
```bash
# Using Command Prompt (Run as Administrator)
net start MySQL80
# or
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe"
```

#### macOS:
```bash
# Using Homebrew
brew services start mysql

# Or manually
mysql.server start
```

#### Linux:
```bash
sudo systemctl start mysql
# or
sudo service mysql start
```

### Step 2: Connect to MySQL

#### Windows Command Prompt:
```bash
mysql -u root -p
# Enter password when prompted
```

#### macOS/Linux Terminal:
```bash
mysql -u root -p
# Enter password when prompted
```

### Step 3: Create Database and User

Execute the following SQL commands in MySQL:

```sql
-- Create database
CREATE DATABASE enterprise_procurement;

-- Verify creation
SHOW DATABASES;

-- Switch to database
USE enterprise_procurement;

-- Create initial roles (Important!)
INSERT INTO role (role_name) VALUES ('ADMIN');
INSERT INTO role (role_name) VALUES ('USER');

-- Verify roles
SELECT * FROM role;
```

**Note**: Tables will be created automatically by Hibernate when the application starts.

### Step 4: Create Database User (Optional but Recommended)

```sql
-- Create a dedicated user for the application
CREATE USER 'eps_user'@'localhost' IDENTIFIED BY 'eps_password_123';

-- Grant privileges
GRANT ALL PRIVILEGES ON enterprise_procurement.* TO 'eps_user'@'localhost';

-- Refresh privileges
FLUSH PRIVILEGES;

-- Verify user creation
SELECT user FROM mysql.user WHERE user='eps_user';
```

---

## Project Configuration

### Step 1: Clone or Download the Project

```bash
# If using Git
git clone <repository-url> enterprise
cd enterprise

# Or extract downloaded ZIP file
```

### Step 2: Update application.properties

Navigate to: `src/main/resources/application.properties`

Update the following properties:

```properties
# ===== DATABASE CONFIGURATION =====

# If using root user:
spring.datasource.url=jdbc:mysql://localhost:3306/enterprise_procurement
spring.datasource.username=root
spring.datasource.password=YOUR_ROOT_PASSWORD

# OR if using dedicated user:
spring.datasource.url=jdbc:mysql://localhost:3306/enterprise_procurement
spring.datasource.username=eps_user
spring.datasource.password=eps_password_123

# Driver
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ===== JPA/HIBERNATE CONFIGURATION =====
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# ===== SERVER CONFIGURATION =====
server.port=8080

# ===== LOGGING =====
logging.level.root=INFO
logging.level.com.eps=DEBUG
```

**Replace `YOUR_ROOT_PASSWORD` with your MySQL root password**

### Step 3: Verify Configuration

Check that:
- MySQL server is running
- Database `enterprise_percussion` exists
- Roles (ADMIN, USER) are inserted
- Credentials in `application.properties` are correct

---

## Building the Application

### Step 1: Open Terminal in Project Directory

```bash
# Navigate to project directory
cd path/to/enterprise

# Verify you're in the correct directory
ls  # macOS/Linux
dir  # Windows
# You should see pom.xml, src, target, etc.
```

### Step 2: Clean and Install Dependencies

```bash
# Clean previous builds
mvn clean

# Download dependencies and build
mvn install

# Or in one command
mvn clean install
```

**This will:**
- Download all Maven dependencies
- Compile the code
- Run unit tests
- Create the JAR file

**Expected Output:**
```
[INFO] BUILD SUCCESS
```

### Step 3: Verify Build

Check the `target` directory:
```bash
ls target/enterprise-procurement-system-1.0.0.jar  # macOS/Linux
dir target\enterprise-procurement-system-1.0.0.jar  # Windows
```

---

## Running the Application

### Option 1: Using Maven (Development)

```bash
mvn spring-boot:run
```

**Expected Output:**
```
Started EnterpriseProcurementSystemApplication in X.XXX seconds
Application is running on: http://localhost:8080
```

### Option 2: Using Java Command (Production)

```bash
java -jar target/enterprise-procurement-system-1.0.0.jar
```

### Option 3: Using IDE

**IntelliJ IDEA:**
1. Right-click on `EnterpriseProcurementSystemApplication.java`
2. Select "Run"

**Eclipse:**
1. Right-click on project
2. Select "Run As" → "Spring Boot App"

**VS Code:**
1. Install Extension Pack for Java
2. Right-click on `EnterpriseProcurementSystemApplication.java`
3. Select "Run"

### Step 4: Verify Application is Running

Open browser and navigate to:
```
http://localhost:8080
```

You should see the Welcome page.

---

## Initial Data Setup

When the application starts:

1. **Hibernate creates tables automatically** based on entities
2. **Roles must be inserted manually** (if not already done)

### Insert Roles via MySQL:

```sql
USE enterprise_percussion;

INSERT INTO role (role_name) VALUES ('ADMIN');
INSERT INTO role (role_name) VALUES ('USER');

SELECT * FROM role;
```

### Verify Tables Created:

```sql
USE enterprise_percussion;

SHOW TABLES;

-- Check table structure
DESCRIBE role;
DESCRIBE user;
```

---

## Postman API Testing

### Step 1: Download and Install Postman

Download from: https://www.postman.com/downloads/

### Step 2: Import Collection

1. Open Postman
2. Click **Import** button (top-left)
3. Select **File** tab
4. Choose file: `postman/Enterprise-Procurement-System.postman_collection.json`
5. Click **Import**

### Step 3: Test API Endpoints

#### Test 1: Register User

1. Click on **Authentication** → **Register User**
2. Update email (use unique email each time)
3. Click **Send**

**Expected Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully"
}
```

#### Test 2: Register Admin

1. Click on **Authentication** → **Register Admin**
2. Update email
3. Click **Send**

**Expected Response (201 Created):**
```json
{
  "success": true,
  "message": "Admin registered successfully"
}
```

#### Test 3: Login

1. Click on **Authentication** → **User Login**
2. Update email and password to match a registered user
3. Click **Send**

**Expected Response (200 OK):**
```json
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

#### Test 4: Admin Dashboard

1. Click on **Admin Dashboard** → **Get Admin Dashboard**
2. Click **Send**

**Expected Response (200 OK):**
```json
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

#### Test 5: User Dashboard

1. Click on **User Dashboard** → **Get User Dashboard**
2. Click **Send**

**Expected Response (200 OK):**
```json
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

### Manual Postman Testing

If you prefer to create requests manually:

**Request 1: Register User**
```
Method: POST
URL: http://localhost:8080/api/auth/register
Headers: Content-Type: application/json
Body:
{
  "fullName": "Test User",
  "email": "test@example.com",
  "password": "TestPass123",
  "phone": "1234567890",
  "address": "Test Address"
}
```

**Request 2: Login**
```
Method: POST
URL: http://localhost:8080/api/auth/login
Headers: Content-Type: application/json
Body:
{
  "email": "test@example.com",
  "password": "TestPass123"
}
```

---

## Frontend Testing

### Step 1: Access Home Page

Open browser: `http://localhost:8080`

### Step 2: Test Registration

1. Click **Register** on home page or go to `http://localhost:8080/register.html`
2. Fill in the form:
   - Full Name: John Doe
   - Email: john@example.com
   - Password: Password123
   - Phone: 1234567890
   - Address: 123 Main Street
3. Click **Register**
4. You should be redirected to login page

### Step 3: Test Login

1. Go to `http://localhost:8080/login.html`
2. Enter credentials:
   - Email: john@example.com
   - Password: Password123
3. Click **Login**
4. If USER role: redirected to `user-dashboard.html`
5. If ADMIN role: redirected to `admin-dashboard.html`

### Step 4: Test Dashboards

- **User Dashboard**: `http://localhost:8080/user-dashboard.html`
  - Shows user information
  - Displays dashboard overview
  - Shows recent activity

- **Admin Dashboard**: `http://localhost:8080/admin-dashboard.html`
  - Shows admin statistics
  - Displays system information
  - Shows admin controls

### Step 5: Test Logout

- Click **Logout** button on any dashboard
- You should be redirected to login page

---

## Running Unit Tests

### Using Maven:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthenticationServiceTest

# Run with TestNG
mvn test -Dsuites=src/test/resources/testng.xml
```

### Using IDE:

**IntelliJ IDEA:**
1. Right-click on test class
2. Select "Run"

**Eclipse:**
1. Right-click on test class
2. Select "Run As" → "JUnit Test"

---

## Troubleshooting

### Issue 1: MySQL Connection Refused

**Error:**
```
Error: Communications link failure
Unable to connect to host 'localhost:3306'
```

**Solution:**
1. Verify MySQL server is running: `mysql --version`
2. Start MySQL server (see [MySQL Database Setup](#mysql-database-setup))
3. Check credentials in `application.properties`
4. Verify database exists: `SHOW DATABASES;`

### Issue 2: Access Denied Error

**Error:**
```
Access denied for user 'root'@'localhost'
```

**Solution:**
1. Check password in `application.properties`
2. Test MySQL connection: `mysql -u root -p`
3. Create new user with correct password (see [MySQL Database Setup](#mysql-database-setup))

### Issue 3: Port Already in Use

**Error:**
```
Port 8080 is already in use
```

**Solution:**
```properties
# Update application.properties
server.port=8081
```

Then restart the application.

### Issue 4: Maven Build Fails

**Error:**
```
BUILD FAILURE
```

**Solution:**
```bash
# Clean Maven cache
mvn clean

# Delete .m2 folder and re-download dependencies
# Windows: %UserProfile%\.m2\repository
# macOS/Linux: ~/.m2/repository

# Rebuild
mvn clean install
```

### Issue 5: Hibernate: "No rows affected"

**Error:**
```
Hibernate DDL Execution Error
```

**Solution:**
```sql
-- Manually insert roles
USE enterprise_percussion;
INSERT INTO role (role_name) VALUES ('ADMIN');
INSERT INTO role (role_name) VALUES ('USER');
```

### Issue 6: Login Always Fails

**Error:**
```
User not found / Invalid password
```

**Solution:**
1. Verify user was registered: `SELECT * FROM user;`
2. Check password validation requirements (8 chars, uppercase, lowercase, number)
3. Clear browser localStorage: Press F12 → Application → LocalStorage → Clear

### Issue 7: Frontend Pages Not Loading

**Error:**
```
404 Not Found
```

**Solution:**
1. Verify app is running on `http://localhost:8080`
2. Check if files exist in `src/main/resources/static/`
3. Restart the application

---

## Performance Optimization

### Enable Query Logging (Development Only)

```properties
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.stat=DEBUG
```

### Disable SQL Formatting (Production)

```properties
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
```

### Connection Pooling

The application uses HikariCP by default (included with Spring Boot).

---

## Security Checklist

- [ ] Change MySQL root password
- [ ] Update `security.jwt.secret` in production
- [ ] Disable `spring.jpa.show-sql` in production
- [ ] Use HTTPS in production
- [ ] Implement rate limiting
- [ ] Add Web Application Firewall (WAF)
- [ ] Regularly update dependencies

---

## Next Steps

1. Explore the codebase
2. Run all tests: `mvn test`
3. Create additional procurement features
4. Deploy to cloud (AWS, Azure, GCP)
5. Set up CI/CD pipeline

---

## Support & Documentation

- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Spring Security**: https://spring.io/projects/spring-security
- **JPA/Hibernate**: https://hibernate.org/orm/
- **MySQL Docs**: https://dev.mysql.com/doc/

---

**Last Updated**: 2026-07-27  
**Version**: 1.0.0
