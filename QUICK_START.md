# Quick Start Guide - Enterprise Procurement System

**Get up and running in 5 minutes!**

---

## Prerequisites Check
```bash
java -version           # Should show Java 21+
mvn --version          # Should show Maven 3.8.0+
mysql --version        # Should show MySQL 8.0+
```

---

## Step 1: MySQL Setup (2 minutes)

**Start MySQL Server:**
```bash
# Windows (as Administrator)
net start MySQL80

# macOS
brew services start mysql

# Linux
sudo systemctl start mysql
```

**Create Database:**
```bash
mysql -u root -p

# In MySQL console:
CREATE DATABASE enterprise_percussion;
USE enterprise_percussion;
INSERT INTO role (role_name) VALUES ('ADMIN');
INSERT INTO role (role_name) VALUES ('USER');
```

---

## Step 2: Configure Application (1 minute)

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/enterprise_percussion
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

Replace `YOUR_PASSWORD` with your MySQL root password.

---

## Step 3: Build Project (1 minute)

```bash
mvn clean install
```

---

## Step 4: Run Application (1 minute)

```bash
mvn spring-boot:run
```

**Expected Output:**
```
Started EnterpriseProcurementSystemApplication in X.XXX seconds
```

---

## Step 5: Test Application

### Browser Access
```
Home:       http://localhost:8080
Register:   http://localhost:8080/register.html
Login:      http://localhost:8080/login.html
```

### Postman Testing
1. Import: `postman/Enterprise-Procurement-System.postman_collection.json`
2. Test endpoints in collection

### Quick API Test
```bash
# Register User
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test User",
    "email": "test@example.com",
    "password": "TestPass123",
    "phone": "1234567890",
    "address": "Test Address"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123"
  }'
```

---

## Common Issues

| Issue | Solution |
|-------|----------|
| MySQL connection refused | Start MySQL server |
| Port 8080 in use | Change in application.properties: `server.port=8081` |
| Build fails | Run `mvn clean install -U` |
| No rows affected | Insert roles manually in database |

---

## Project Files Overview

```
enterprise/
├── README.md              ← Full documentation
├── SETUP_GUIDE.md         ← Detailed setup
├── PROJECT_SUMMARY.md     ← Implementation details
├── pom.xml               ← Dependencies
├── src/main/            ← Source code
├── src/test/            ← Unit tests
└── postman/             ← API collection
```

---

## Useful Commands

```bash
# Build only
mvn clean package

# Run tests
mvn test

# Run specific test
mvn test -Dtest=AuthenticationServiceTest

# Run without building
mvn spring-boot:run

# Package as JAR
mvn clean package -DskipTests

# Run packaged JAR
java -jar target/enterprise-procurement-system-1.0.0.jar
```

---

## Default Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| /api/auth/register | POST | Register USER |
| /api/auth/admin/register | POST | Register ADMIN |
| /api/auth/login | POST | User login |
| /admin/dashboard | GET | Admin dashboard |
| /user/dashboard | GET | User dashboard |

---

## Stop Application

```bash
# Press Ctrl+C in terminal
```

---

## Next: Full Setup?

See **SETUP_GUIDE.md** for:
- Detailed MySQL setup
- Advanced configuration
- Troubleshooting
- Production deployment

---

**Ready to go!** 🚀

For more details, see README.md or SETUP_GUIDE.md
