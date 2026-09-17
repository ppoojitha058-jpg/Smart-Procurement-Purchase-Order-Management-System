# Enterprise Procurement System (EPS) — Complete Flow, Roles, Tech Stack, Design & Architecture

## 1. Project Summary
Enterprise Procurement System is a full-stack, role-based procurement lifecycle manager for a corporate enterprise. It covers: catalog browsing → purchase requisition → manager approval → procurement RFQ/quotation → purchase order + supplier assignment → admin/finance payment → supplier fulfillment (accept/dispatch/deliver) → receiving GRN → invoice/payment → notifications, audit, CSV reports.

Seeded demo accounts (from `DataInitializer.java`, passwords BCrypt-encoded):
- `admin@eps.com / Admin@123` → ADMIN
- `employee@eps.com / User@123` → USER (John Doe, IT dept)
- `manager@eps.com / Manager@123` → MANAGER (Sarah Jenkins, IT dept)
- `procurement@eps.com / Procurement@123` → PROCUREMENT
- `supplier@eps.com / Supplier@123` → SUPPLIER (Acme / Apex Technologies Global)
- `receiving@eps.com / Receiving@123` → RECEIVING
- `finance@eps.com / Finance@123` → FINANCE
- 6 departments (IT, Operations & Facilities, Finance & Accounting, HR, Marketing, Engineering), 8 categories, 20 products with SKU/image/price/qty.

Run: `cd enterprise; mvn spring-boot:run` → `http://localhost:8080/login.html` (JWT + session). DB: MySQL `enterprise_percussion` (`ddl-auto=update`).

## 2. Tech Stack
- **Backend:** Java 17, Spring Boot 3.3.1, Spring Web, Spring Data JPA/Hibernate, Spring Security + JWT (jjwt 0.12.3), Validation, Spring Mail (Gmail SMTP), WebSocket/STOMP (`/ws-procurement`), Actuator, ModelMapper 3.1.1, Lombok, Commons-CSV 1.10.0, DevTools.
- **Frontend:** Static HTML/CSS/JS in `src/main/resources/static/` — `login.html/register.html/index.html/dashboard.html (role-router) + user-dashboard.html/manager-dashboard.html/admin-dashboard.html/supplier-view.html`, `main_dashboard.js + dashboard_extracted.js + js/ + css/`. No build step; fetch + localStorage JWT.
- **DB:** MySQL 8 (`mysql-connector-j`), Hibernate MySQL8Dialect.
- **Build/Test:** Maven, surefire + TestNG (`testng.xml`), Spring Boot Test + Security Test.
- **Auth:** BCrypt + `JwtUtil` + `JwtAuthenticationFilter` (stateless, `SessionCreationPolicy.STATELESS`), `SecurityConfig` URL rules + `@PreAuthorize` method security, CORS for 3000/8080.

## 3. System Architecture (Layered Monolith)
```
Browser (HTML/JS) ──fetch/JWT──> Controller (REST) ──> Service ──> Repository (JPA) ──> MySQL
                                      │                    ├── EmailService (SMTP)
                                      │                    ├── NotificationService (DB + WebSocket)
                                      │                    ├── AuditLogService
                                      │                    └── CsvExportService
Security: JwtAuthenticationFilter → DaoAuthenticationProvider → @PreAuthorize / URL matchers
Static hosting: Spring serves /static/*.html directly; /api/** is secured API.
```
- **Controller layer (15 controllers):** `Authentication, Product, Category, Department, Supplier, PurchaseRequest, Manager, Procurement, Admin, SupplierPortal, Receiving, Finance, User, Notification, CsvReport, SupplierFeedback`.
- **Service layer (19 services):** `Authentication, User, Role, Product, Category, Department, Supplier, PurchaseRequest, PurchaseOrder, Quotation, Rfq, Invoice, Payment, GoodsReceipt, Notification, Email, AuditLog, CsvExport`.
- **Entity layer (15 entities):** `User, Role, Department, Category, Product, Supplier, PurchaseRequest, PurchaseOrder, Quotation, Rfq, Invoice, Payment, GoodsReceipt, Notification, AuditLog, SupplierFeedback`.
- **DTOs:** `ApiResponseDto{success,message,data}, ProductDto, PurchaseRequestDto, SupplierDto, CategoryDto, DepartmentDto, UserLogin/Response/RegistrationDto, ManagerActionDto`.
- **Cross-cutting:** `GlobalExceptionHandler, ModelMapperConfig, WebSocketConfig, DataInitializer (seed)`.

## 4. System Design
- **RBAC:** Roles ADMIN/USER/MANAGER/PROCUREMENT/SUPPLIER/RECEIVING/FINANCE. URL rules in `SecurityConfig` + fine-grained `@PreAuthorize("hasAnyRole(...)")` + ownership checks (`ensureEligibleOwner`, `resolveAuthenticatedSupplier`, `scopedRequests` by manager department, supplier isolation).
- **State machines:**
  - `PurchaseRequest: CREATED/PENDING → APPROVED/REJECTED → ORDERED (+ PAID after payment) → DELIVERED/COMPLETED`
  - `PurchaseOrder: CREATED/ASSIGNED → ACCEPTED/REJECTED → PAID (admin settled) → PROCESSING → SHIPPED/DISPATCHED → DELIVERED/COMPLETED`
  - `Invoice: PENDING/SUBMITTED → APPROVED → PAID`, `Payment: PENDING → COMPLETED`, `Product: ACTIVE/INACTIVE`, `Supplier: ACTIVE/INACTIVE`.
- **Key invariants:** Supplier sees only `findBySupplier(supplier)` orders; manager sees only own-department requests; user sees only own requests/orders; admin bypasses scoping.
- **Notifications/Audit:** Every transition writes `Notification{email,role,isRead}` + `AuditLog`; supplier dashboard counts `unreadNotifications`.
- **Reports:** `CsvReportController` streams CSV per role (user/manager/admin/supplier tracking, payments, deliveries).

## 5. Who Does What (Role-wise)
- **USER (Employee) — `UserController + PurchaseRequestController`:** Browse catalog (`GET /api/products` public), create requisition `POST /api/purchase-requests`, view `GET /api/users/my-requests|my-orders|stats`, track `GET /api/users/track/{orderId}`, submit supplier feedback, download own CSVs, receive notifications. Dashboard: stats, Active Ordered Products, Recent Activity, catalog, tracking, reports (Budget Guardian hidden).
- **MANAGER — `ManagerController`:** `GET /api/manager/pending|department-requests|approval-history|dashboard-stats`, `POST /api/manager/approve/{id}|reject/{id}` (department-scoped; admin sees all). Also `getMyRequests`, `getDepartmentOrders`. Owns approval queue for IT dept (Sarah Jenkins).
- **PROCUREMENT — `ProcurementController`:** `GET /api/procurement/approved-requests`, `POST /api/procurement/rfq`, `GET /api/procurement/rfqs|/{id}|/{id}/quotes`, `POST /api/procurement/select/{id}/{quotationId}` → creates `PurchaseOrder`, `GET /api/procurement/orders|/{id}`.
- **ADMIN — `AdminController + Product/Category/Department/Supplier controllers`:** `GET /api/admin/dashboard-stats|requests|orders|payments|suppliers|deliveries|users`, `POST /api/admin/process/{id}`, `POST /api/admin/orders/{id}/assign-supplier`, `POST /api/admin/orders/{id}/complete-payment|/requests/{id}/complete-payment` (sets Order=PAID, Request=ORDERED, creates Invoice+Payment COMPLETED). Manages master data (CRUD products/categories/departments/suppliers), registers managers, all CSVs.
- **SUPPLIER — `SupplierPortalController`:** `GET /api/supplier/dashboard-stats` (assigned/pending/accepted/processing/shipped/delivered/paidReady/pendingPayments/completedPayments/unread), `GET /api/supplier/orders` (isolated, includes PAID), `POST /api/supplier/orders/{id}/accept|reject|status|shipment|deliver`, `GET /api/supplier/payments|rfqs/open|quotes|invoices`, `POST /api/supplier/rfqs/{id}/quote|/orders/{id}/invoice`, product catalog CRUD (own products only). Flow: ACCEPT → (PAID ready) → Dispatch/PROCESSING → SHIPPED → DELIVERED.
- **RECEIVING — `ReceivingController`:** `GET /api/receiving/orders`, `POST /api/receiving/grn`, `GET /api/receiving/grns|/order/{orderId}` (GRN after delivery).
- **FINANCE — `FinanceController`:** `GET /api/finance/invoices|/{id}|payments|/{id}`, `POST /api/finance/invoices/{id}/match` (3-way match), `POST /api/finance/invoices/{id}/status|/process-payment`.
- **ALL:** `AuthenticationController (/api/auth/register|register-admin|login → JWT)`, `NotificationController (/api/notifications/my|/{id}/read|/read-all)`.

## 6. End-to-End Flows
**A. Happy-path procurement:**
1. USER creates `PurchaseRequest(CREATED)` with product/dept/qty.
2. MANAGER approves → `APPROVED` (or rejects → `REJECTED`).
3. PROCUREMENT creates `Rfq` for approved request → SUPPLIERs submit `Quotation`.
4. PROCUREMENT selects quote → `PurchaseOrder(CREATED/ASSIGNED)` linked to supplier+request.
5. ADMIN assigns supplier (if needed) + completes payment → `Order=PAID, Request=ORDERED, Invoice+Payment(COMPLETED)`.
6. SUPPLIER accepts → `ACCEPTED`, dispatches → `PROCESSING/SHIPPED`, delivers → `DELIVERED`.
7. RECEIVING creates `GoodsReceipt (GRN)`; FINANCE matches invoice + closes payment; USER tracks + gives feedback.

**B. Supplier paid-flow (recent fix):** PAID was invisible (only ASSIGNED/CREATED/ACCEPTED counted). Now `dashboard-stats: accepted=ACCEPTED/PAID/ORDERED, processing=PROCESSING/PAID/ORDERED + paidReadyOrders`, Dispatch button shows for ACCEPTED/PAID/ORDERED/PROCESSING as `Dispatch (Paid)`.

**C. Auth flow:** Register (BCrypt) → Login → `JwtUtil` issues token → `JwtAuthenticationFilter` validates per request → `SecurityContext` → controller `@PreAuthorize` + ownership check → service → JPA → MySQL. Frontend stores JWT in localStorage, sends `Authorization: Bearer`.

## 7. API Map (prefix → who)
- `/api/auth/**` public; `/api/products|/categories|/departments|/suppliers GET` public, write ADMIN.
- `/api/purchase-requests/**` USER,ADMIN; `/api/users/**` USER,ADMIN; `/api/manager/**` MANAGER,ADMIN; `/api/procurement/**` PROCUREMENT,ADMIN,SUPPLIER; `/api/supplier/**` SUPPLIER,ADMIN,PROCUREMENT,FINANCE; `/api/receiving/**` RECEIVING,ADMIN,PROCUREMENT,FINANCE; `/api/finance/**` FINANCE,ADMIN; `/api/admin/**` ADMIN; `/api/notifications/**` authenticated; `/api/reports/csv/**` authenticated (role-scoped inside).

## 8. Frontend Routing
`login.html → JWT → dashboard.html` router reads role → renders user/manager/admin/supplier sections (single-page with role divs) + dedicated `user-dashboard.html/manager-dashboard.html/admin-dashboard.html/supplier-view.html` legacy pages. `main_dashboard.js` handles stats, orders, shipments, payments, tracking, CSV downloads, notifications polling, WebSocket toasts.

## 9. How to Demo (2 min)
1. `mvn spring-boot:run`, open `/login.html`.
2. USER `employee@eps.com`: create request → logout.
3. MANAGER `manager@eps.com`: approve → logout.
4. PROCUREMENT: RFQ → select quote → logout.
5. ADMIN `admin@eps.com`: assign supplier + Pay → logout.
6. SUPPLIER `supplier@eps.com`: Accept → Dispatch (Paid) → Mark Delivered → invoice → logout.
7. RECEIVING: GRN; FINANCE: match + pay; USER: track + feedback + CSV.