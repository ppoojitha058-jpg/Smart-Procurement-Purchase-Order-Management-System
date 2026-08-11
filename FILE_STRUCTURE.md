# Enterprise Procurement System - Complete File Structure & Documentation Index

## 📋 Documentation Files

| File | Purpose | Read Time |
|------|---------|-----------|
| **QUICK_START.md** | Get running in 5 minutes | 5 min |
| **SETUP_GUIDE.md** | Detailed setup & MySQL instructions | 20 min |
| **README.md** | Project overview & API documentation | 15 min |
| **PROJECT_SUMMARY.md** | Implementation details & checklist | 10 min |
| **this file** | File structure overview | 5 min |

👉 **Start here**: QUICK_START.md

---

## 🏗️ Project Structure

### Root Level Files
```
enterprise/
├── pom.xml                          Maven configuration
├── .gitignore                       Git ignore patterns
│
├── QUICK_START.md                   Quick 5-minute setup
├── SETUP_GUIDE.md                   Complete setup guide
├── README.md                        Project documentation
├── PROJECT_SUMMARY.md               Implementation summary
└── FILE_STRUCTURE.md                This file
```

### Source Code
```
src/main/java/com/eps/
│
├── EnterpriseProcurementSystemApplication.java
│   └── Main application entry point
│
├── config/
│   ├── SecurityConfig.java
│   │   └── Spring Security configuration with BCrypt
│   └── ModelMapperConfig.java
│       └── ModelMapper bean configuration
│
├── controller/
│   ├── AuthenticationController.java
│   │   ├── POST /api/auth/register         (Register USER)
│   │   ├── POST /api/auth/admin/register   (Register ADMIN)
│   │   └── POST /api/auth/login            (User login)
│   │
│   ├── AdminController.java
│   │   └── GET /admin/dashboard            (Admin dashboard)
│   │
│   └── UserController.java
│       └── GET /user/dashboard             (User dashboard)
│
├── dto/
│   ├── UserRegistrationDto.java
│   │   └── Registration form input DTO
│   │
│   ├── UserLoginDto.java
│   │   └── Login form input DTO
│   │
│   ├── UserLoginResponseDto.java
│   │   └── Login response with user details
│   │
│   └── ApiResponseDto.java
│       └── Generic API response wrapper
│
├── entity/
│   ├── Role.java
│   │   └── JPA Entity for roles (ADMIN, USER)
│   │
│   └── User.java
│       └── JPA Entity for users with relationships
│
├── exception/
│   ├── UserNotFoundException.java
│   │   └── Custom exception - user not found
│   │
│   ├── EmailAlreadyExistsException.java
│   │   └── Custom exception - email exists
│   │
│   ├── InvalidPasswordException.java
│   │   └── Custom exception - invalid password
│   │
│   └── GlobalExceptionHandler.java
│       └── Centralized exception handling
│
├── repository/
│   ├── RoleRepository.java
│   │   └── JpaRepository for Role entity
│   │
│   └── UserRepository.java
│       ├── findByEmail(String email)
│       └── existsByEmail(String email)
│
└── service/
    ├── AuthenticationService.java (Interface)
    │   ├── registerUser()
    │   ├── registerAdmin()
    │   └── login()
    │
    ├── UserService.java (Interface)
    │   ├── getUserByEmail()
    │   ├── emailExists()
    │   ├── saveUser()
    │   ├── getUserById()
    │   └── getAllUsers()
    │
    ├── RoleService.java (Interface)
    │   ├── getRoleByName()
    │   ├── saveRole()
    │   └── getRoleById()
    │
    └── impl/
        ├── AuthenticationServiceImpl.java
        │   └── Main business logic for auth
        │
        ├── UserServiceImpl.java
        │   └── User repository operations
        │
        └── RoleServiceImpl.java
            └── Role repository operations
```

### Resources
```
src/main/resources/
│
├── application.properties
│   ├── Server: port 8080
│   ├── Database: enterprise_percussion
│   ├── User: root
│   ├── Password: [YOUR_PASSWORD]
│   └── JPA: hibernate.ddl-auto=update
│
└── static/
    ├── index.html
    │   └── Home/Welcome page
    │
    ├── login.html
    │   ├── User & Admin login
    │   ├── Email/Password input
    │   └── Role-based redirection
    │
    ├── register.html
    │   ├── User registration form
    │   ├── Full name, email, password, phone, address
    │   └── Password validation display
    │
    ├── admin-dashboard.html
    │   ├── Admin sidebar navigation
    │   ├── System statistics cards
    │   ├── System information display
    │   └── Logout button
    │
    └── user-dashboard.html
        ├── User sidebar navigation
        ├── User profile information
        ├── Dashboard overview
        └── Recent activity section
```

### Tests
```
src/test/
│
├── java/com/eps/
│   │
│   ├── service/impl/
│   │   ├── AuthenticationServiceTest.java
│   │   │   ├── testRegisterUserSuccess()
│   │   │   ├── testRegisterUserEmailAlreadyExists()
│   │   │   ├── testLoginSuccess()
│   │   │   ├── testLoginUserNotFound()
│   │   │   └── testLoginInvalidPassword()
│   │   │
│   │   └── UserServiceTest.java
│   │       ├── testGetUserByEmailSuccess()
│   │       ├── testGetUserByEmailNotFound()
│   │       ├── testEmailExistsTrue()
│   │       ├── testEmailExistsFalse()
│   │       ├── testSaveUserSuccess()
│   │       └── testGetUserByIdSuccess()
│   │
│   └── repository/
│       ├── UserRepositoryTest.java
│       │   ├── testFindByEmailSuccess()
│       │   ├── testFindByEmailNotFound()
│       │   ├── testExistsByEmailTrue()
│       │   └── testExistsByEmailFalse()
│       │
│       └── RoleRepositoryTest.java
│           ├── testFindByRoleNameAdmin()
│           ├── testFindByRoleNameUser()
│           └── testFindByRoleNameNotFound()
│
└── resources/
    └── testng.xml
        └── TestNG test suite configuration
```

### Postman
```
postman/
└── Enterprise-Procurement-System.postman_collection.json
    ├── Authentication
    │   ├── Register User
    │   ├── Register Admin
    │   └── User Login
    │
    ├── Admin Dashboard
    │   └── Get Admin Dashboard
    │
    └── User Dashboard
        └── Get User Dashboard
```

---

## 🔍 Quick Navigation

### To understand the project:
1. **README.md** - Overview and architecture
2. **PROJECT_SUMMARY.md** - All implemented features

### To set up the project:
1. **QUICK_START.md** - 5-minute setup
2. **SETUP_GUIDE.md** - Detailed setup with MySQL

### To use APIs:
1. **README.md** - API Endpoints section
2. **postman/collection.json** - Import into Postman

### To develop:
1. **src/main/java/com/eps/** - Source code structure
2. **pom.xml** - Dependencies list
3. **application.properties** - Configuration

### To test:
1. **src/test/java/com/eps/** - Unit tests
2. Run: `mvn test`

---

## 📊 Key Endpoints Summary

| Endpoint | Method | Auth Required | Role | Purpose |
|----------|--------|---------------|------|---------|
| `/api/auth/register` | POST | No | - | Register USER |
| `/api/auth/admin/register` | POST | No | ADMIN | Register ADMIN |
| `/api/auth/login` | POST | No | - | Login user |
| `/admin/dashboard` | GET | Yes | ADMIN | View admin dashboard |
| `/user/dashboard` | GET | Yes | USER | View user dashboard |

---

## 🗂️ Database Structure

### Role Table
- **Columns**: role_id (PK), role_name
- **Data**: ADMIN, USER

### User Table
- **Columns**: user_id (PK), full_name, email (UNIQUE), password, phone, address, enabled, created_date, updated_date, role_id (FK)
- **Relationship**: Many-to-One with Role

---

## 🛠️ Important Configuration

**application.properties Location:**
```
src/main/resources/application.properties
```

**Must Update:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/enterprise_percussion
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD    ← Change this
```

---

## 📦 Maven Dependencies

| Group | Artifact | Version | Purpose |
|-------|----------|---------|---------|
| org.springframework.boot | spring-boot-starter-web | 3.3.1 | REST APIs |
| org.springframework.boot | spring-boot-starter-data-jpa | 3.3.1 | Database ORM |
| org.springframework.boot | spring-boot-starter-security | 3.3.1 | Authentication |
| com.mysql | mysql-connector-java | 8.0.33 | MySQL driver |
| org.projectlombok | lombok | Latest | Reduce boilerplate |
| org.modelmapper | modelmapper | 3.1.1 | DTO mapping |
| org.testng | testng | 7.9.1 | Unit testing |
| io.jsonwebtoken | jjwt-* | 0.12.3 | JWT support |

---

## 🚀 Running the Application

**Minimum Commands:**
```bash
# Setup
mvn clean install

# Run
mvn spring-boot:run

# Access
http://localhost:8080
```

**Alternative - Run JAR:**
```bash
mvn clean package -DskipTests
java -jar target/enterprise-procurement-system-1.0.0.jar
```

---

## ✅ Verification Checklist

- [ ] Java 21 installed
- [ ] Maven 3.8.0+ installed
- [ ] MySQL server running
- [ ] Database created: `enterprise_percussion`
- [ ] Roles inserted (ADMIN, USER)
- [ ] application.properties updated
- [ ] Build successful: `mvn clean install`
- [ ] App running: `mvn spring-boot:run`
- [ ] Home page loads: `http://localhost:8080`

---

## 📞 Documentation Quick Links

| Document | Contains |
|----------|----------|
| QUICK_START.md | 5-minute setup |
| SETUP_GUIDE.md | Complete installation guide |
| README.md | Project features & APIs |
| PROJECT_SUMMARY.md | Implementation checklist |

---

## 🔐 Security Highlights

✅ BCrypt password encryption  
✅ Role-based access control  
✅ Input validation  
✅ CORS configuration  
✅ Exception handling  
✅ SQL injection prevention (JPA)  

---

## 📈 Scalability Ready

✅ Layered architecture  
✅ Separated concerns  
✅ Repository pattern  
✅ Dependency injection  
✅ Service abstraction  

---

**Version**: 1.0.0  
**Last Updated**: 2026-07-27  
**Status**: ✅ Production Ready

---

👉 **Next Step**: Read QUICK_START.md to get started!
