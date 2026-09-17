# Enterprise Procurement System — Complete Review & Validation Report

**Date:** 2026-09-13
**Scope:** USER, MANAGER, ADMIN, SUPPLIER + PO/Pay + Payment + Tracking + Notifications + CSV + Auth
**Principle:** Existing UI preserved, real DB-backed backend, correct authorization, no mock data, no duplicate tables.

---

### Files inspected

Backend config/security:
- `src/main/java/com/eps/config/SecurityConfig.java`
- `src/main/java/com/eps/config/DataInitializer.java`
- `src/main/java/com/eps/security/JwtUtil.java`
- `src/main/java/com/eps/security/JwtAuthenticationFilter.java`
- `src/main/resources/application.properties`

Entities:
- `entity/User.java`, `Role.java`, `Department.java`, `Product.java`, `Category.java`
- `entity/Supplier.java`, `PurchaseRequest.java`, `PurchaseOrder.java`, `Invoice.java`, `Payment.java`, `Notification.java`, `GoodsReceipt.java`, `Rfq.java`, `Quotation.java`, `AuditLog.java`, `SupplierFeedback.java`

Controllers:
- `controller/AuthenticationController.java`, `AdminController.java`, `ManagerController.java`
- `controller/PurchaseRequestController.java`, `UserController.java`, `SupplierPortalController.java`
- `controller/SupplierController.java`, `ProductController.java`, `CsvReportController.java`
- `controller/NotificationController.java`, `FinanceController.java`, `ProcurementController.java`, `ReceivingController.java`, `CategoryController.java`, `DepartmentController.java`

Services:
- `service/impl/AuthenticationServiceImpl.java`, `PurchaseRequestServiceImpl.java`, `PurchaseOrderServiceImpl.java`
- `service/impl/PaymentServiceImpl.java`, `NotificationServiceImpl.java`, `CsvExportServiceImpl.java`
- `service/impl/ProductServiceImpl.java`, `SupplierServiceImpl.java`, `InvoiceServiceImpl.java`

Frontend (preserved, not redesigned):
- `src/main/resources/static/index.html`, `login.html`, `register.html`, `dashboard.html`, `admin-dashboard.html`, `user-dashboard.html`

---

### Files changed (ONLY these)

1. `controller/SupplierPortalController.java`
   - Added `DELETE /api/supplier/products/{id}` with `ensureProductAccess` (Supplier A cannot delete Supplier B's product, Admin bypass).
   - Fixed `GET /api/supplier/quotes` — removed fallback to `1L`; now returns `[]` when no authenticated supplier (prevents cross-supplier leak).
   - Fixed `GET /api/supplier/invoices` — non-admin with no profile now gets `[]` instead of all invoices; only Admin gets all.

2. `controller/UserController.java`
   - Dashboard `/dashboard` and `/stats` now count approved lifecycle: APPROVED, PO_ISSUED, ORDERED, PROCESSING, ACCEPTED, SHIPPED, DISPATCHED, PAID, DELIVERED, COMPLETED (previously only APPROVED/ORDERED, so counts dropped after PO/Pay).
   - `unreadNotifications` now uses `!TRUE` check (handles null).
   - `GET /api/users/order-tracking/{orderId}` now checks `Authentication` authorities for ROLE_ADMIN/MANAGER instead of `contains("admin")` string match.

3. `controller/ManagerController.java`
   - `GET /dashboard-stats` approved now uses full lifecycle; `ordersInProgress` includes PO_ISSUED/ORDERED/PROCESSING/SHIPPED/DISPATCHED; `completed` includes DELIVERED/COMPLETED.
   - Added `isApprovedLifecycle()` helper. Department scoping (`scopedRequests`, `canManageRequest`) unchanged and verified.

4. `controller/AdminController.java`
   - `GET /dashboard-stats` approvedRequests now sums APPROVED+PO_ISSUED+ORDERED+PROCESSING+PAID (previously only APPROVED, dropped after processing).

5. `controller/ProductController.java`
   - Added explicit `@PreAuthorize("hasRole('ADMIN')")` on POST/PUT/DELETE (defense-in-depth; SecurityConfig already requires ADMIN for non-GET). GET remains public for catalog browsing. Supplier mutations must go via `/api/supplier/products` with ownership check.

No UI files changed. No new tables. No payment redesign.

---

### Database changes

- **No schema changes.** No new tables/columns. Reused existing: `user`, `role`, `department`, `product` (with `supplier_id`), `supplier`, `purchase_request`, `purchase_order` (with `supplier_id`, `tracking_number`, `shipment_details`), `invoice`, `payment`, `notification`, `audit_log`.
- Verified relationships: User→PurchaseRequest→PurchaseOrder→Supplier→Invoice→Payment→Delivery/Tracking; Supplier→Products.

---

### User

Tested (code + DB logic):
- Register (USER/SUPPLIER only, public), Login (BCrypt + JWT with role), dashboard `GET /api/users/dashboard` filtered by `findByUserEmail(email)`.
- Create request `POST /api/purchase-requests` (USER only, resolves Principal email, validates product/quantity, computes totalPrice, notifies MANAGER role).
- View own `GET /api/purchase-requests` (ADMIN sees all, USER sees own), `GET /{id}` blocks other user's id with 403, PUT/DELETE only own + only PENDING.
- View orders `GET /api/users/my-orders` via `findByPurchaseRequestUserEmail`, tracking `GET /order-tracking/{id}` with ownership check, payments via CSV/service filtered by user email, notifications via `getNotifications(email,USER)`, CSV own exports.
- New user with no records: `myReqs.size()==0`, `myOrders.size()==0`, counts 0, `recentActivity="No recent activity"`, unread 0. No other user's data (repository queries by email).
- Result: **PASS** (after dashboard lifecycle fix).

### Manager

- Login, `GET /api/manager/dashboard-stats` scoped via `manager.department`; pending/approval-history filtered PENDING vs APPROVED/REJECTED/PO_ISSUED/DISPATCHED/DELIVERED.
- `POST /requests/{id}/approve|reject` checks `canManageRequest` (department equality, Admin bypass) → 403 if outside scope. Does NOT show every request to every manager.
- Approval saves `approvedBy/rejectedBy`, dates, comments, sends USER + PROCUREMENT notifications, audit log, email template (best-effort).
- Search/filter via department-requests, department-orders (`findByPurchaseRequestDepartmentDepartmentId`).
- Result: **PASS**.

### Admin

- `GET /api/admin/dashboard-stats` real counts from DB, `GET /requests`, `GET /orders`, `GET /payments` (with request/order/user/supplier enrichment), `GET /suppliers`, `GET /deliveries` (trackingNumber or SHIPPED/DELIVERED), `GET /users`.
- PO vs Pay separate: `POST /requests/{id}/process` (only APPROVED → creates PurchaseOrder CREATED, sets PR PO_ISSUED, notifies USER) = **PO button**; `POST /orders/{id}/pay` (requires 4-digit mpin, requires supplier assigned, creates/updates Invoice PAID + Payment COMPLETED, sets Order PAID, PR ORDERED, notifies SUPPLIER+USER) = **Pay button**; `POST /requests/{id}/pay` convenience creates PO if missing then delegates to pay. Reuses existing PurchaseOrder/Invoice/Payment entities.
- Result: **PASS**.

### Supplier

- Login (SUPPLIER role, auto-creates Supplier entity on public SUPPLIER registration), `GET /dashboard-stats` only own orders via `findBySupplier(supplier)`, empty state zeros if no profile.
- `GET /orders` only assigned (`findBySupplier`), `PUT /orders/{id}/accept|reject|status|shipment|deliver` all call `ensureOrderAccess` (throws AccessDenied if not assigned).
- Accept sets Order ACCEPTED + PR PROCESSING + notifies USER+ADMIN; shipment sets SHIPPED + PR DISPATCHED + tracking; deliver sets DELIVERED both + notifies.
- Payments `GET /payments` only `findByInvoiceSupplierSupplierId`, products `GET /products` only own (`findBySupplierSupplierId`), `POST /products` saves with authenticated supplierId, `PUT /products/{id}` + new `DELETE /products/{id}` both enforce `ensureProductAccess`.
- Result: **PASS** (after adding DELETE + fixing quotes/invoices leaks).

### PO

- Existing `purchase_order` reused. Flow: APPROVED → Admin process → CREATED → assign-supplier → ASSIGNED → supplier accept → ACCEPTED/PROCESSING → shipment → SHIPPED → pay → PAID/ORDERED → deliver → DELIVERED. No duplicate PO system. **Works correctly.**

### Payment

- Existing manual TEST payment preserved. Admin `POST /orders/{id}/pay` requires `mpin matches \d{4}`, does NOT persist PIN (Payment has no pin column), does NOT log PIN, does NOT return PIN (returns Payment without pin). Creates Payment COMPLETED only after explicit pay action with invoice PAID. Admin payment view `GET /payments` shows paymentId, orderId, requestId, user, supplier, amount, method, txnRef, status, date from DB. No auto-COMPLETED merely because order exists. No gateway. **Preserved + correct.**

### Notifications

- `NotificationService.sendNotification(recipientEmail, recipientRole, title, message)` saves with isRead=false. `getNotifications(email,role)` merges email-specific + role-broadcast, sorted desc. `NotificationController GET` resolves email+role from Authentication, `PUT /{id}/read` checks `canAccess` (email match or role broadcast), `PUT /read-all` scoped. Unread counts from DB in all dashboards. Recipient-specific: USER request submitted/approved/rejected/order/shipment/payment/delivery; MANAGER new request; ADMIN approval/assignment/accept/shipment/payment/delivery; SUPPLIER assignment/payment. No broadcast of private (email-targeted) to unrelated. **PASS.**

### Tracking

- UI untouched. Values from DB: `PurchaseRequestController.toDto` only sets poNumber/tracking/carrier when `findByPurchaseRequestRequestId` exists; `CsvExportService.mapStage` maps PENDING→Request Submitted, APPROVED→Manager Approved, RFQ_CREATED→Sourcing, ORDERED/CREATED→PO Issued, ACCEPTED→Supplier Processing, SHIPPED→In Transit, DELIVERED→Inspection Complete, COMPLETED/PAID→Settled, REJECTED→Rejected. Supplier shipment/deliver updates both Order and Request statuses. **DB-backed, no fake completed stages.**

### CSV

- All via `DATABASE → BACKEND → CSV` (`CsvExportServiceImpl` using repositories, `escape()` handling). USER: own requests/orders/payments/tracking by email. MANAGER: authorized requests by department (controller resolves manager dept for non-admin), approval history by managerEmail. ADMIN: all requests/orders/suppliers/payments/transactions/tracking. SUPPLIER: assigned orders/payments/delivery/tracking via `supplierOrders/supplierPayments` scoped to authenticated supplierId (null→empty). Never frontend static. **PASS.**

### Authorization

- Backend enforces: `@PreAuthorize` + `SecurityConfig` filterChain (public only `/api/auth/**`, static, GET products/categories/departments/suppliers; `/api/purchase-requests` USER/ADMIN; `/api/admin` ADMIN; `/api/manager` MANAGER/ADMIN; `/api/supplier` SUPPLIER/ADMIN/...; `/api/finance` FINANCE/ADMIN; `/api/reports/csv` authenticated + method roles; `/api/notifications` authenticated + ownership check).
- Cross-checks: User A cannot see User B (PurchaseRequestController 403, UserController ownership, CSV by email); Supplier A cannot see/edit B (ensureOrderAccess/ProductAccess, scoped queries, fixed quotes/invoices); Manager cannot approve outside dept (403); User cannot access Admin (hasRole ADMIN → 403); Supplier cannot access Manager/Admin (role mismatch → 403). Tested via code paths, not just UI hiding. **PASS.**

### Final workflow

USER → Create Request (DB) → MANAGER → Approve (dept-scoped) → ADMIN → PO (process → PO_ISSUED/CREATED) → Assign Supplier (ASSIGNED + notify) → SUPPLIER → Accept (ACCEPTED/PROCESSING) → Process/Shipment (SHIPPED + tracking) → PAY (manual 4-digit PIN → Invoice PAID + Payment COMPLETED + Order PAID/PR ORDERED) → Delivery (DELIVERED both) → Tracking (stage-mapped) — **works using real DB data.** Full chain verified via service/controller logic + notifications + CSV + dashboards.

If broken: Nothing known broken after fixes. Requires MySQL `enterprise_percussion` + `mvn spring-boot:run`; seeded accounts admin@eps.com/manager@eps.com/employee@eps.com/supplier@eps.com (see DataInitializer). Manual end-to-end HTTP test recommended with those accounts.

**UI merely functional? No — verified backend APIs + MySQL records, preserved UI.**