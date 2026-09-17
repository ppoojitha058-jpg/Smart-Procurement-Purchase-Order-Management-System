# Enterprise Procurement & Purchase Order Management System (EPS)

A production-ready, full-stack enterprise procurement management platform built with **Java 17**, **Spring Boot 3.3.1**, **Spring Security (JWT)**, **Spring Data JPA (Hibernate)**, **MySQL 8**, and a responsive **HTML5/CSS3/Vanilla JavaScript** frontend.

---

## 1. Project Overview

The **Enterprise Procurement & Purchase Order Management System (EPS)** is an automated, role-based requisition and order lifecycle management solution designed for corporate enterprises. It bridges the gap between employees requesting goods, department managers reviewing budgets, procurement specialists issuing Requests for Quotation (RFQs), suppliers fulfilling orders, warehouse receiving staff verifying shipments, and finance teams executing 3-way invoice matching and payments.

The entire frontend is served directly as static web resources by Spring Boot, requiring zero Node.js/npm dependencies, while communicating with a secured RESTful API backed by JSON Web Tokens (JWT).

---

## 2. Problem Statement

In conventional corporate procurement environments:
- **Disjointed Processes:** Purchasing requisitions, supplier quotes, purchase orders, receiving notes, and vendor invoices exist in siloed spreadsheets, paperwork, and emails.
- **Approval Bottlenecks:** Lack of departmental scoping prevents department managers from efficiently monitoring their team's spend against departmental budgets.
- **Opaque Supplier Communication:** Suppliers lack direct portals to submit quotations, accept purchase orders, update shipping tracking numbers, or check invoice payment statuses.
- **Financial Leakage:** Manual reconciliation between purchase orders, warehouse goods receipt notes (GRNs), and supplier invoices leads to overpayment and payment inaccuracies.
- **Audit Deficits:** Absence of centralized audit trails makes compliance reporting and state transition tracking difficult.

---

## 3. Objectives

- **Automate the Requisition-to-Pay Lifecycle:** Deliver a seamless end-to-end workflow from purchase request creation to invoice settlement.
- **Enforce Role-Based Access Control (RBAC):** Provide 7 dedicated roles with fine-grained authorization and departmental isolation.
- **Enable Real-Time Visibility:** Provide in-app WebSocket (STOMP) notifications and transactional HTML email alerts for every order transition.
- **Implement 3-Way Matching:** Protect enterprise cash flow by matching Purchase Orders, Goods Receipt Notes (GRN), and Supplier Invoices before payment disbursement.
- **Empower Vendor Accountability:** Provide supplier performance scoring and employee feedback ratings (1–5 stars with reviews).
- **Zero-Friction Deployment:** Deliver a monolithic Spring Boot architecture where frontend static assets are hosted without complex client-side build steps.

---

## 4. Key Features

- **Multi-Role RBAC (7 Roles):** Dedicated dashboards and permissions for Employee (`USER`), `MANAGER`, `PROCUREMENT`, `SUPPLIER`, `RECEIVING`, `FINANCE`, and `ADMIN`.
- **Product & Category Catalog:** Browse active products by category with price, SKU, stock quantity, and product status (`ACTIVE`/`INACTIVE`).
- **Departmental Budget & Scoping:** Department managers can only inspect and approve requisitions submitted by employees within their assigned department.
- **RFQ & Quotation Engine:** Procurement teams issue RFQs from approved requisitions; suppliers bid competitive quotes; procurement selects the winning quotation.
- **Purchase Order (PO) Management:** Automated generation of POs linked to approved requisitions and assigned suppliers.
- **Supplier Portal:** Isolated portal where suppliers view only their assigned orders, submit quotes, accept/reject POs, generate invoices, update tracking numbers, and dispatch shipments.
- **Warehouse Goods Receipt (GRN):** Receiving staff record incoming shipments, inspect received quantities, verify product condition, and issue Goods Receipt Notes.
- **Finance & 3-Way Matching:** Finance teams cross-verify Purchase Orders, GRNs, and Supplier Invoices before approving and completing payments.
- **Supplier Performance & Ratings:** Employees submit 1 to 5 star ratings with detailed comments post-delivery, feeding directly into supplier performance metrics.
- **Dual Notification Channels:**
  - **In-App Real-Time Alerts:** Powered by Spring WebSocket and STOMP (`/ws-procurement`, `/topic/notifications`).
  - **Email Notifications:** Transactional HTML emails sent via Spring Mail / SMTP using pre-configured templates.
- **Audit Logging & CSV Exports:** Centralized `AuditLog` entity capturing every user action, paired with role-scoped CSV report streaming.

---

## 5. Technology Stack

### Backend
| Component | Technology | Version |
|---|---|---|
| **Language** | Java | 17 (LTS) |
| **Framework** | Spring Boot | 3.3.1 |
| **Security** | Spring Security + JWT | JJWT 0.12.3 |
| **Persistence** | Spring Data JPA / Hibernate | 6.x |
| **Database** | MySQL Server | 8.0+ |
| **Database Driver** | MySQL Connector/J | 8.3.0 |
| **WebSocket** | Spring WebSocket + STOMP | 3.3.1 |
| **Email** | Spring Boot Starter Mail | 3.3.1 |
| **Object Mapping** | ModelMapper | 3.1.1 |
| **Boilerplate Reduction** | Project Lombok | 1.18.38 |
| **Reporting** | Apache Commons CSV | 1.10.0 |
| **Monitoring** | Spring Boot Actuator | 3.3.1 |
| **Build Tool** | Apache Maven | 3.8+ |
| **Testing** | TestNG & Spring Boot Test | TestNG 7.10.0 |

### Frontend
| Component | Technology | Description |
|---|---|---|
| **Architecture** | Vanilla HTML5 / CSS3 / JavaScript | Zero-build static architecture |
| **Serving** | Spring Boot Static Resources | Hosted from `src/main/resources/static/` |
| **Icons** | Lucide Icons (`lucide.min.js`) | Modern iconography |
| **Real-Time Client** | SockJS (`sockjs.min.js`) & STOMP (`stomp.umd.min.js`) | WebSocket client for live toast notifications |
| **Styling** | Modern CSS Design System (`css/style.css`) | Glassmorphism, cards, responsive data tables |
| **State & Auth** | JavaScript `localStorage` | Client-side JWT bearer token storage |

---

## 6. System Architecture

EPS is architected as a clean layered monolith designed for maintainability, security, and low operational overhead:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      CLIENT LAYER (Browser / Mobile)                         │
│  Static HTML5 + Modern CSS + Vanilla JS (localStorage JWT, Fetch API, SockJS)│
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ HTTP REST / WebSocket (STOMP)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      SECURITY & INTERCEPTION LAYER                          │
│  JwtAuthenticationFilter ──▶ DaoAuthenticationProvider ──▶ @PreAuthorize    │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Authenticated Principal & DTOs
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      REST CONTROLLER LAYER (16 Controllers)                 │
│  Admin, Auth, Category, CsvReport, Department, Finance, Manager,             │
│  Notification, Procurement, Product, PurchaseRequest, Receiving, Supplier,  │
│  SupplierFeedback, SupplierPortal, User                                      │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      SERVICE BUSINESS LAYER (18 Services)                   │
│  AuthenticationService, PurchaseRequestService, PurchaseOrderService,       │
│  SupplierPortalService, FinanceService, GoodsReceiptService, etc.           │
│                                                                             │
│  Cross-Cutting Integrations:                                                │
│  ├── EmailService (Spring Mail / SMTP HTML Templates)                      │
│  ├── NotificationService (WebSocket Broker + In-App DB Storage)            │
│  ├── AuditLogService (Audit Trail Logging)                                  │
│  └── CsvExportService (Commons CSV Exporter)                                │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      DATA ACCESS LAYER (16 Spring Data JPA Repos)           │
│  UserRepository, PurchaseRequestRepository, PurchaseOrderRepository, etc.   │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Hibernate ORM / SQL
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      DATABASE LAYER (MySQL 8)                               │
│  Database: enterprise_percussion                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. User Roles & Seeded Credentials

EPS includes a built-in database seeder (`DataInitializer.java`) that automatically provisions default roles, sample departments, product categories, catalog items, and pre-configured user accounts on first startup.

| Role | Email | Password | Assigned Department / Entity | Primary Responsibility |
|---|---|---|---|---|
| **ADMIN** | `admin@eps.com` | `Admin@123` | Executive / IT | System administration, user/role management, master data CRUD, direct payments |
| **USER** (Employee) | `employee@eps.com` | `User@123` | IT Department | Browse catalog, submit requisitions, track orders, rate suppliers |
| **MANAGER** | `manager@eps.com` | `Manager@123` | IT Department | Review, approve, or reject department purchase requests |
| **PROCUREMENT** | `procurement@eps.com` | `Procurement@123` | Procurement & Sourcing | Issue RFQs, review supplier quotes, issue Purchase Orders |
| **SUPPLIER** | `supplier@eps.com` | `Supplier@123` | Apex Technologies Global | Submit quotes, accept/reject POs, dispatch shipments, track payments |
| **RECEIVING** | `receiving@eps.com` | `Receiving@123` | Warehouse / Logistics | Inspect incoming deliveries, record received quantities, issue GRNs |
| **FINANCE** | `finance@eps.com` | `Finance@123` | Finance & Accounting | Execute 3-way matching (PO vs GRN vs Invoice), settle payments |

---

## 8. Complete Procurement Workflow

```
[ Employee (USER) ]
        │  1. Submits Purchase Request (status: CREATED)
        ▼
[ Department MANAGER ]
        │  2. Reviews & Approves Requisition (status: APPROVED)
        ▼
[ PROCUREMENT ]
        │  3. Issues RFQ (Request for Quotation)
        ▼
[ SUPPLIER ]
        │  4. Submits Quotation with competitive unit pricing
        ▼
[ PROCUREMENT ]
        │  5. Evaluates & Selects Quote ──▶ Generates Purchase Order (status: CREATED/ASSIGNED)
        ▼
[ ADMIN / FINANCE ]
        │  6. Authorizes initial order settlement (Order: PAID, Request: ORDERED)
        ▼
[ SUPPLIER ]
        │  7. Accepts Order ──▶ Dispatches with Courier & Tracking # (status: SHIPPED)
        ▼
[ RECEIVING (Warehouse) ]
        │  8. Inspects goods upon arrival ──▶ Generates Goods Receipt Note (GRN)
        ▼
[ SUPPLIER ]
        │  9. Submits digital Invoice against Purchase Order
        ▼
[ FINANCE ]
        │  10. Executes 3-Way Match (PO == GRN == Invoice) ──▶ Settle Payment (status: COMPLETED)
        ▼
[ Employee (USER) ]
           11. Receives goods, tracks delivery, and submits Supplier Rating (1-5 Stars)
```

---

## 9. Project Directory Structure

```
Development-of-Smart-Procurement-Purchase-Order-Management-System/
├── frontend/                                # Static Web Client (HTML5 / Modern CSS / Vanilla JS)
│   ├── *.html                               # Role-based dashboards & authentication pages
│   ├── main_dashboard.js                    # Core frontend client controller
│   ├── css/                                 # Styling & theme design system
│   └── js/                                  # Lucide icons, SockJS, & STOMP clients
│
├── backend/                                 # Spring Boot Backend (Java 17, Spring Boot 3.3.1)
│   ├── pom.xml                              # Maven build dependencies & plugins
│   ├── application.properties.example       # Sanitized environment template for Git
│   ├── *.ps1                                # Automated E2E integration & verification scripts
│   └── src/
│       ├── main/
│       │   ├── java/com/eps/
│       │   │   ├── config/                  # Security, WebSockets, ModelMapper, DataInitializer
│       │   │   ├── controller/              # 16 REST Controllers (Admin, User, Supplier, etc.)
│       │   │   ├── service/                 # Business logic interfaces & implementations
│       │   │   ├── repository/              # 16 Spring Data JPA repositories
│       │   │   ├── entity/                  # 16 JPA database entities
│       │   │   ├── dto/                     # Request/response transfer objects
│       │   │   ├── security/                # JWT filters and token provider
│       │   │   └── exception/               # Global exception handlers
│       │   └── resources/
│       │       ├── application.properties   # Local config (untracked in git)
│       │       └── mail-templates/          # 6 Transactional HTML email templates
│       └── test/                            # TestNG unit & integration test suites
│
├── database/                                # Database Schemas
│   └── init_enterprise_percussion.sql       # MySQL schema & database initialization script
│
├── docs/                                    # Documentation & Guides
│   ├── PROJECT_FLOW_ARCHITECTURE.md         # End-to-end architecture & flow document
│   ├── SETUP_GUIDE.md                       # Installation & setup guide
│   ├── FILE_STRUCTURE.md                    # Detailed file breakdown
│   └── *.md                                 # Quick start, summaries, & validation reports
│
├── postman/                                 # API Testing
│   └── Enterprise-Procurement-System.postman_collection.json
│
├── README.md                                # Repository documentation
└── .gitignore                               # Git ignore rules for Spring Boot, IDE, & secrets
```

---

## 10. Database Information

- **Database Engine:** MySQL 8.0 or higher
- **Default Database Name:** `enterprise_percussion`
- **Schema Management:** Automated via Hibernate (`spring.jpa.hibernate.ddl-auto=update`)
- **Seeded Data:** Handled on startup by `DataInitializer.java`:
  - 7 Roles (`ADMIN`, `USER`, `MANAGER`, `PROCUREMENT`, `SUPPLIER`, `RECEIVING`, `FINANCE`)
  - 6 Corporate Departments (IT, Operations & Facilities, Finance & Accounting, HR, Marketing, Engineering)
  - 8 Product Categories (Electronics, Office Supplies, Hardware, Software, Furniture, Cloud Services, Networking, Security)
  - 20 Pre-configured Products with realistic pricing, SKU codes, and initial stock quantities
  - Default demo users with BCrypt-hashed passwords for all 7 roles

---

## 11. Backend Setup

### Prerequisites
- **Java Development Kit (JDK):** Version 17 (or 21) installed. Verify with:
  ```bash
  java -version
  ```
- **Apache Maven:** Version 3.8.0 or higher installed. Verify with:
  ```bash
  mvn -version
  ```
- **MySQL Server:** Version 8.0 or higher running on port `3306`.

### Step 1: Create the MySQL Database
Log in to your MySQL terminal or MySQL Workbench and run:
```sql
CREATE DATABASE IF NOT EXISTS enterprise_percussion;
```
*(Optionally initialize schema using the script in `database/init_enterprise_percussion.sql`)*

### Step 2: Configure Environment Properties
Navigate to `backend/` and copy `application.properties.example` to create your local `application.properties`:
```bash
cd backend
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Update your local credentials in `backend/src/main/resources/application.properties`:
```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/enterprise_percussion?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

# JWT Secret (Generate a strong 256-bit secret)
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
jwt.expiration=86400000

# Mail Configuration (Optional - for real email alerts)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-gmail-app-password
```

---

## 12. Frontend Setup

**No Node.js, npm, or frontend build tool is required.**

The frontend consists of modern Vanilla HTML5, CSS3, and JavaScript located inside the dedicated **`frontend/`** directory. Spring Boot is pre-configured via `spring.web.resources.static-locations=file:../frontend/,file:./frontend/,classpath:/static/` to serve these assets directly at the root URL (`http://localhost:8080/`). When you start the Spring Boot application, the frontend is immediately accessible in any modern web browser.

---

## 13. How to Run the Application

### Option A: Using Maven (Recommended for Development)
Navigate to the `backend/` directory and run:
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

### Option B: Building and Running as an Executable JAR
```bash
cd backend
mvn clean package -DskipTests
java -jar target/enterprise-procurement-system-1.0.0.jar
```

### Step 3: Access the Web Application
Open your browser and navigate to:
```
http://localhost:8080/login.html
```

Use any of the seeded credentials from the **User Roles** table above to log in.

---

## 14. Key API Endpoints

### Authentication (`/api/auth`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register new employee (`USER`) |
| `POST` | `/api/auth/admin/register` | ADMIN | Register a new administrator |
| `POST` | `/api/auth/login` | Public | Authenticate user and receive JWT bearer token |

### Catalog & Master Data
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/products` | Public | Retrieve all available products |
| `POST` | `/api/products` | ADMIN | Add new catalog product |
| `GET` | `/api/categories` | Public | Retrieve all product categories |
| `GET` | `/api/departments` | Public | Retrieve all corporate departments |
| `GET` | `/api/suppliers` | Public | Retrieve active suppliers |

### Requisitions & Approvals
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/purchase-requests` | USER, ADMIN | Submit a new purchase requisition |
| `GET` | `/api/purchase-requests/my-requests` | USER, ADMIN | List current user's submitted requisitions |
| `GET` | `/api/manager/pending` | MANAGER, ADMIN | List pending requisitions for manager's department |
| `POST` | `/api/manager/approve/{id}` | MANAGER, ADMIN | Approve requisition |
| `POST` | `/api/manager/reject/{id}` | MANAGER, ADMIN | Reject requisition with reason |

### Procurement & Sourcing
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/procurement/approved-requests` | PROCUREMENT, ADMIN | List approved requests ready for RFQ |
| `POST` | `/api/procurement/rfq` | PROCUREMENT, ADMIN | Issue RFQ for an approved request |
| `GET` | `/api/procurement/rfqs/{id}/quotes` | PROCUREMENT, ADMIN | View supplier quotes for an RFQ |
| `POST` | `/api/procurement/select/{id}/{quotationId}` | PROCUREMENT, ADMIN | Select winning quote and issue PO |

### Supplier Portal (`/api/supplier`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/supplier/dashboard-stats` | SUPPLIER | Retrieve supplier order and revenue metrics |
| `GET` | `/api/supplier/orders` | SUPPLIER | List assigned purchase orders |
| `POST` | `/api/supplier/orders/{id}/accept` | SUPPLIER | Accept purchase order |
| `POST` | `/api/supplier/orders/{id}/shipment` | SUPPLIER | Record carrier and tracking number |
| `POST` | `/api/supplier/orders/{id}/deliver` | SUPPLIER | Mark order delivered to warehouse |
| `POST` | `/api/supplier/orders/{id}/invoice` | SUPPLIER | Submit digital invoice for payment |

### Receiving & Goods Receipt (`/api/receiving`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/receiving/orders` | RECEIVING, ADMIN | List delivered orders awaiting inspection |
| `POST` | `/api/receiving/grn` | RECEIVING, ADMIN | Issue Goods Receipt Note (GRN) after inspection |

### Finance & Settlements (`/api/finance`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/finance/invoices` | FINANCE, ADMIN | List invoices pending review |
| `POST` | `/api/finance/invoices/{id}/match` | FINANCE, ADMIN | Perform automated 3-Way Match (PO vs GRN vs Invoice) |
| `POST` | `/api/finance/invoices/{id}/process-payment` | FINANCE, ADMIN | Authorize and disburse invoice payment |

### Tracking & Ratings
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/users/track/{orderId}` | USER, ADMIN | Retrieve real-time shipment milestone tracking |
| `POST` | `/api/supplier-feedback` | USER, ADMIN | Submit 1–5 star supplier rating and feedback |
| `GET` | `/api/supplier-feedback/supplier/{supplierId}` | Public | View public feedback for a supplier |

### Real-Time Notifications & Reports
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/notifications/my` | Authenticated | Retrieve in-app notifications |
| `PUT` | `/api/notifications/{id}/read` | Authenticated | Mark notification as read |
| `GET` | `/api/reports/csv/user-requests` | USER, ADMIN | Download user requests as CSV |
| `GET` | `/api/reports/csv/admin-orders` | ADMIN | Export all purchase orders as CSV |

---

## 15. Security & Authentication

- **Stateless Architecture:** EPS utilizes `SessionCreationPolicy.STATELESS`. No server-side HTTP session state is stored.
- **JWT Authentication:**
  1. Client sends credentials via `POST /api/auth/login`.
  2. `AuthenticationService` verifies credentials against BCrypt-hashed passwords.
  3. `JwtUtil` generates a signed HMAC-SHA256 token containing the user's email, role, and expiration timestamp.
  4. On subsequent requests, `JwtAuthenticationFilter` extracts the `Authorization: Bearer <token>` header, validates the signature, and populates the `SecurityContextHolder`.
- **Method-Level Security:** High-privilege controller endpoints are protected using `@PreAuthorize("hasRole('...')")` and `@PreAuthorize("hasAnyRole(...)")`.
- **Departmental & Entity Scoping:** Services actively enforce that managers only inspect requests belonging to their department and suppliers only access their assigned orders.
- **CORS Configuration:** Configured in `SecurityConfig.java` to allow local cross-origin integration during testing.

---

## 16. Testing

### Running Unit & Integration Tests
EPS uses **TestNG** alongside Spring Boot Test for testing service and repository layers:
```bash
# Run all tests configured in testng.xml
mvn test

# Run tests with detailed surefire output
mvn test -DsuiteXmlFile=src/test/resources/testng.xml
```

### End-to-End PowerShell Integration Test
EPS includes an automated PowerShell integration script (`test_workflow.ps1`) that executes the entire 11-step procurement lifecycle against a running server:
```powershell
# In PowerShell (with application running on localhost:8080)
.\test_workflow.ps1
```

### Postman Collection
Import `postman/Enterprise-Procurement-System.postman_collection.json` into Postman to test pre-configured API requests across all user roles.

---

## 17. Future Enhancements

- **Multi-Currency Support:** Add dynamic currency conversion for international vendor billing.
- **Automated Invoice OCR:** Scan uploaded PDF vendor invoices using Tesseract/OCR to auto-populate invoice line items.
- **PDF Purchase Order & GRN Generation:** Generate downloadable, digitally signed PDF documents for POs and Goods Receipt Notes using iText or OpenPDF.
- **Progressive Web App (PWA):** Add service workers and offline catalog caching for warehouse receiving staff on mobile devices.
- **Docker & Container Orchestration:** Provide multi-stage `Dockerfile` and `docker-compose.yml` for unified deployment with MySQL.

---

## 18. License

This project is developed for enterprise procurement management and is available for internal evaluation and commercial use. All rights reserved.
