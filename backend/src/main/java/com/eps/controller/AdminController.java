package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.dto.UserRegistrationDto;
import com.eps.entity.*;
import com.eps.repository.*;
import com.eps.service.AuthenticationService;
import com.eps.service.NotificationService;
import com.eps.service.PurchaseOrderService;
import com.eps.service.PurchaseRequestService;
import com.eps.service.SupplierService;
import com.eps.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Admin Controller
 * Production-ready real database operations for the Admin role:
 * Governance, Procurement Pipeline, Supplier Assignment, Order Management, Payments, User Management
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    private final PurchaseRequestService purchaseRequestService;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierService supplierService;
    private final SupplierRepository supplierRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationService notificationService;

    @Data
    public static class AssignSupplierRequest {
        private Long supplierId;
    }

    @Data
    public static class ProcessPaymentRequest {
        private String paymentMethod;
        private String transactionReference;
        private BigDecimal amount;
        private String mpin;
    }

    /**
     * Real database Dashboard Statistics for Admin
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponseDto> getAdminDashboardStats() {
        log.info("Fetching real Admin dashboard statistics from database");

        long totalUsers = userRepository.count();
        long totalManagers = userRepository.countByRoleRoleName("MANAGER");
        long totalSuppliers = supplierRepository.count();

        long totalRequests = purchaseRequestRepository.count();
        long pendingApprovals = purchaseRequestRepository.countByStatus("PENDING");
        long approvedRequests = purchaseRequestRepository.countByStatus("APPROVED")
                + purchaseRequestRepository.countByStatus("PO_ISSUED")
                + purchaseRequestRepository.countByStatus("ORDERED")
                + purchaseRequestRepository.countByStatus("PROCESSING")
                + purchaseRequestRepository.countByStatus("PAID");
        long rejectedRequests = purchaseRequestRepository.countByStatus("REJECTED");

        List<PurchaseOrder> allOrders = purchaseOrderRepository.findAll();
        long activeOrders = allOrders.stream()
                .filter(o -> !"DELIVERED".equalsIgnoreCase(o.getStatus()))
                .count();
        long completedOrders = allOrders.stream()
                .filter(o -> "DELIVERED".equalsIgnoreCase(o.getStatus()))
                .count();

        List<Invoice> allInvoices = invoiceRepository.findAll();
        long pendingPayments = allInvoices.stream()
                .filter(i -> !"PAID".equalsIgnoreCase(i.getStatus()))
                .count();
        long completedPayments = paymentRepository.countByStatus("COMPLETED");

        double totalSpend = purchaseRequestRepository.findAll().stream()
                .mapToDouble(r -> r.getTotalPrice() != null ? r.getTotalPrice().doubleValue() : 0.0)
                .sum();

        long pendingDeliveries = allOrders.stream()
                .filter(o -> "SHIPPED".equalsIgnoreCase(o.getStatus()) || "DISPATCHED".equalsIgnoreCase(o.getStatus()) || "IN_TRANSIT".equalsIgnoreCase(o.getStatus()))
                .count();
        long completedDeliveries = completedOrders;

        long unreadNotifications = notificationService.getNotifications(null, "ADMIN").stream()
                .filter(n -> !Boolean.TRUE.equals(n.getIsRead()))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalManagers", totalManagers);
        stats.put("totalSuppliers", totalSuppliers);
        stats.put("totalPurchaseRequests", totalRequests);
        stats.put("pendingApprovals", pendingApprovals);
        stats.put("approvedRequests", approvedRequests);
        stats.put("rejectedRequests", rejectedRequests);
        stats.put("activeOrders", activeOrders);
        stats.put("completedOrders", completedOrders);
        stats.put("pendingPayments", pendingPayments);
        stats.put("completedPayments", completedPayments);
        stats.put("totalProcurementValue", totalSpend);
        stats.put("pendingDeliveries", pendingDeliveries);
        stats.put("completedDeliveries", completedDeliveries);
        stats.put("unreadNotifications", unreadNotifications);

        ApiResponseDto response = ApiResponseDto.builder()
                .success(true)
                .message("Admin dashboard stats fetched")
                .data(stats)
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Backward-compatibility endpoint for /api/admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseDto> legacyAdminDashboard() {
        return getAdminDashboardStats();
    }

    /**
     * All purchase requests queue with full relation details
     */
    @GetMapping("/requests")
    public ResponseEntity<ApiResponseDto> getAllRequests() {
        List<PurchaseRequest> requests = purchaseRequestRepository.findAll();
        List<Map<String, Object>> result = requests.stream().map(this::formatRequestMap).toList();
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Requests fetched").data(result).build());
    }

    /**
     * Process an approved request: Creates a real PurchaseOrder in DB
     */
    @PostMapping("/requests/{id}/process")
    public ResponseEntity<ApiResponseDto> processRequest(@PathVariable Long id) {
        PurchaseRequest pr = purchaseRequestService.getPurchaseRequestById(id);
        if (!"APPROVED".equalsIgnoreCase(pr.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponseDto.builder().success(false).message("Only approved requests can be processed into orders").build());
        }

        PurchaseOrder order = purchaseOrderRepository.findByPurchaseRequestRequestId(id)
                .orElseGet(() -> {
                    PurchaseOrder po = new PurchaseOrder();
                    po.setPurchaseRequest(pr);
                    po.setTotalAmount(pr.getTotalPrice());
                    po.setStatus("CREATED");
                    return purchaseOrderRepository.save(po);
                });

        pr.setStatus("PO_ISSUED");
        purchaseRequestRepository.save(pr);

        if (pr.getUser() != null) {
            notificationService.sendNotification(
                    pr.getUser().getEmail(),
                    "USER",
                    "Purchase Order Created for Requisition #" + pr.getRequestId(),
                    "Admin has processed your approved requisition #" + pr.getRequestId() + " into Purchase Order PO-" + order.getOrderId()
            );
        }

        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Requisition processed into Purchase Order").data(formatOrderMap(order)).build());
    }

    /**
     * Get all purchase orders
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponseDto> getAllOrders() {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll();
        List<Map<String, Object>> result = orders.stream().map(this::formatOrderMap).toList();
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Orders fetched").data(result).build());
    }

    /**
     * Assign a supplier to an order
     */
    @PostMapping("/orders/{id}/assign-supplier")
    public ResponseEntity<ApiResponseDto> assignSupplier(@PathVariable Long id, @RequestBody AssignSupplierRequest request) {
        if (request.getSupplierId() == null) {
            return ResponseEntity.badRequest().body(ApiResponseDto.builder().success(false).message("Supplier ID is required").build());
        }

        PurchaseOrder order = purchaseOrderService.getOrderById(id);
        Supplier supplier = supplierService.getSupplierById(request.getSupplierId());

        order.setSupplier(supplier);
        order.setStatus("ASSIGNED");
        PurchaseOrder saved = purchaseOrderRepository.save(order);

        // Notify Supplier
        if (supplier.getEmail() != null) {
            notificationService.sendNotification(
                    supplier.getEmail(),
                    "SUPPLIER",
                    "New Procurement Order Assigned: ORD-" + order.getOrderId(),
                    "Admin assigned Order ORD-" + order.getOrderId() + " (" + (order.getPurchaseRequest() != null && order.getPurchaseRequest().getProduct() != null ? order.getPurchaseRequest().getProduct().getName() : "Goods") + ") to you for fulfillment."
            );
        }

        // Notify User
        if (order.getPurchaseRequest() != null && order.getPurchaseRequest().getUser() != null) {
            notificationService.sendNotification(
                    order.getPurchaseRequest().getUser().getEmail(),
                    "USER",
                    "Supplier Assigned for Request #" + order.getPurchaseRequest().getRequestId(),
                    "Your requisition #" + order.getPurchaseRequest().getRequestId() + " has been assigned to supplier " + supplier.getName() + "."
            );
        }

        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Supplier successfully assigned to order").data(formatOrderMap(saved)).build());
    }

    /**
     * Complete payment for an order
     */
    @PostMapping("/orders/{id}/pay")
    public ResponseEntity<ApiResponseDto> completeOrderPayment(
            @PathVariable Long id,
            @RequestBody(required = false) ProcessPaymentRequest request) {
        PurchaseOrder order = purchaseOrderService.getOrderById(id);

        if (request == null || request.getMpin() == null || !request.getMpin().matches("\\d{4}")) {
            return ResponseEntity.badRequest().body(ApiResponseDto.builder().success(false)
                    .message("A valid 4-digit payment PIN is required").build());
        }

        if (order.getSupplier() == null) {
            return ResponseEntity.badRequest().body(ApiResponseDto.builder().success(false)
                    .message("Assign a supplier before completing payment").build());
        }

        PurchaseOrder finalOrder = order;
        Invoice invoice = invoiceRepository.findByPurchaseOrderOrderId(order.getOrderId())
                .orElseGet(() -> {
                    Invoice inv = new Invoice();
                    inv.setPurchaseOrder(finalOrder);
                    inv.setSupplier(finalOrder.getSupplier());
                    inv.setInvoiceNumber("INV-" + System.currentTimeMillis() % 1000000);
                    inv.setAmount(finalOrder.getTotalAmount() != null ? finalOrder.getTotalAmount() : BigDecimal.ZERO);
                    inv.setStatus("PAID");
                    return invoiceRepository.save(inv);
                });
        invoice.setStatus("PAID");
        invoiceRepository.save(invoice);

        String method = (request != null && request.getPaymentMethod() != null) ? request.getPaymentMethod() : "BANK_TRANSFER";
        String txnRef = (request != null && request.getTransactionReference() != null && !request.getTransactionReference().isBlank())
                ? request.getTransactionReference() : "TXN-" + System.currentTimeMillis();
        BigDecimal amount = (request != null && request.getAmount() != null) ? request.getAmount() : invoice.getAmount();

        Payment payment = paymentRepository.findByInvoiceInvoiceId(invoice.getInvoiceId())
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setInvoice(invoice);
                    p.setAmount(amount);
                    p.setPaymentMethod(method);
                    p.setTransactionReference(txnRef);
                    p.setStatus("COMPLETED");
                    return paymentRepository.save(p);
                });
        payment.setStatus("COMPLETED");
        payment.setStatus("COMPLETED");
        payment.setPaymentDate(LocalDateTime.now());
        payment.setTransactionReference(txnRef);
        Payment savedPayment = paymentRepository.save(payment);

        // Update Order status to PAID
        order.setStatus("PAID");
        purchaseOrderRepository.save(order);

        // Update Associated Purchase Request status to ORDERED
        if (order.getPurchaseRequest() != null) {
            PurchaseRequest pr = order.getPurchaseRequest();
            pr.setStatus("ORDERED");
            purchaseRequestRepository.save(pr);
        }

        // Notify Supplier
        if (order.getSupplier() != null && order.getSupplier().getEmail() != null) {
            notificationService.sendNotification(
                    order.getSupplier().getEmail(),
                    "SUPPLIER",
                    "Payment Received for ORD-" + order.getOrderId() + " (" + method + ")",
                    "Payment of ₹" + amount + " for order ORD-" + order.getOrderId() + " has been settled via " + method + ". Reference: " + txnRef + ". Ready for dispatch!"
            );
        }

        // Notify Requester
        if (order.getPurchaseRequest() != null && order.getPurchaseRequest().getUser() != null) {
            notificationService.sendNotification(
                    order.getPurchaseRequest().getUser().getEmail(),
                    "USER",
                    "Payment Settled for Requisition #" + order.getPurchaseRequest().getRequestId(),
                    "Payment of ₹" + amount + " for your requisition #" + order.getPurchaseRequest().getRequestId() + " has been settled via " + method + " (Ref: " + txnRef + "). Order is being fulfilled."
            );
        }

        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Payment processed successfully").data(savedPayment).build());
    }

    /**
     * Direct Payment for a Purchase Request by Admin (requires PO to be issued first)
     */
    @PostMapping("/requests/{id}/pay")
    public ResponseEntity<ApiResponseDto> completeRequestPayment(
            @PathVariable Long id,
            @RequestBody(required = false) ProcessPaymentRequest request) {
        PurchaseRequest pr = purchaseRequestService.getPurchaseRequestById(id);

        PurchaseOrder order = purchaseOrderRepository.findByPurchaseRequestRequestId(id).orElse(null);
        if (order == null) {
            return ResponseEntity.badRequest().body(ApiResponseDto.builder().success(false)
                    .message("Purchase Order must be issued before payment can be made").build());
        }

        if (order.getSupplier() == null) {
            supplierRepository.findAll().stream().findFirst().ifPresent(order::setSupplier);
            purchaseOrderRepository.save(order);
        }

        return completeOrderPayment(order.getOrderId(), request);
    }

    /**
     * All payments list
     */
    @GetMapping("/payments")
    public ResponseEntity<ApiResponseDto> getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();
        List<Map<String, Object>> result = payments.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("paymentId", p.getPaymentId());
            map.put("amount", p.getAmount());
            map.put("paymentDate", p.getPaymentDate());
            map.put("paymentMethod", p.getPaymentMethod());
            map.put("transactionReference", p.getTransactionReference());
            map.put("status", p.getStatus());
            if (p.getInvoice() != null) {
                map.put("invoiceNumber", p.getInvoice().getInvoiceNumber());
                if (p.getInvoice().getPurchaseOrder() != null) {
                    map.put("orderId", p.getInvoice().getPurchaseOrder().getOrderId());
                    if (p.getInvoice().getPurchaseOrder().getPurchaseRequest() != null) {
                        map.put("requestId", p.getInvoice().getPurchaseOrder().getPurchaseRequest().getRequestId());
                        if (p.getInvoice().getPurchaseOrder().getPurchaseRequest().getUser() != null) {
                            map.put("userName", p.getInvoice().getPurchaseOrder().getPurchaseRequest().getUser().getFullName());
                        }
                    }
                }
                if (p.getInvoice().getSupplier() != null) {
                    map.put("supplierName", p.getInvoice().getSupplier().getName());
                }
            }
            return map;
        }).toList();
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Payments fetched").data(result).build());
    }

    /**
     * All suppliers
     */
    @GetMapping("/suppliers")
    public ResponseEntity<ApiResponseDto> getSuppliers() {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Suppliers fetched").data(suppliers).build());
    }

    /**
     * All deliveries & tracking
     */
    @GetMapping("/deliveries")
    public ResponseEntity<ApiResponseDto> getDeliveries() {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll().stream()
                .filter(o -> o.getTrackingNumber() != null || "SHIPPED".equalsIgnoreCase(o.getStatus()) || "DELIVERED".equalsIgnoreCase(o.getStatus()))
                .toList();
        List<Map<String, Object>> result = orders.stream().map(this::formatOrderMap).toList();
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Deliveries fetched").data(result).build());
    }

    /**
     * Real database users
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponseDto> listUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        for (User user : userService.getAllUsers()) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getUserId());
            userData.put("fullName", user.getFullName());
            userData.put("email", user.getEmail());
            userData.put("phone", user.getPhone());
            userData.put("enabled", user.getEnabled());
            userData.put("role", user.getRole() != null ? user.getRole().getRoleName() : "USER");
            userData.put("departmentName", user.getDepartment() != null ? user.getDepartment().getDepartmentName() : "General");
            users.add(userData);
        }
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Users loaded successfully").data(users).build());
    }

    @PostMapping("/manager/register")
    public ResponseEntity<ApiResponseDto> registerManager(@RequestBody UserRegistrationDto registrationDto) {
        try {
            String message = authenticationService.registerManager(registrationDto);
            return new ResponseEntity<>(ApiResponseDto.builder().success(true).message(message).build(), HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error registering manager: {}", e.getMessage());
            throw e;
        }
    }

    private Map<String, Object> formatRequestMap(PurchaseRequest r) {
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", r.getRequestId());
        map.put("quantity", r.getQuantity());
        map.put("totalPrice", r.getTotalPrice());
        map.put("status", r.getStatus());
        map.put("approvedBy", r.getApprovedBy());
        map.put("approvalDate", r.getApprovalDate());
        map.put("rejectedBy", r.getRejectedBy());
        map.put("rejectionDate", r.getRejectionDate());
        map.put("managerComments", r.getManagerComments());
        map.put("createdDate", r.getCreatedDate());
        if (r.getUser() != null) {
            map.put("userId", r.getUser().getUserId());
            map.put("userName", r.getUser().getFullName());
            map.put("userEmail", r.getUser().getEmail());
        }
        if (r.getDepartment() != null) {
            map.put("departmentId", r.getDepartment().getDepartmentId());
            map.put("departmentName", r.getDepartment().getDepartmentName());
        }
        if (r.getProduct() != null) {
            map.put("productId", r.getProduct().getProductId());
            map.put("productName", r.getProduct().getName());
            map.put("pricePerProduct", r.getProduct().getPricePerProduct());
            map.put("sku", r.getProduct().getSku());
            if (r.getProduct().getCategory() != null) {
                map.put("categoryName", r.getProduct().getCategory().getCategoryName());
            }
        }
        return map;
    }

    private Map<String, Object> formatOrderMap(PurchaseOrder o) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", o.getOrderId());
        map.put("totalAmount", o.getTotalAmount());
        map.put("status", o.getStatus());
        map.put("trackingNumber", o.getTrackingNumber());
        map.put("shipmentDetails", o.getShipmentDetails());
        map.put("createdDate", o.getCreatedDate());
        map.put("updatedDate", o.getUpdatedDate());
        if (o.getSupplier() != null) {
            map.put("supplierId", o.getSupplier().getSupplierId());
            map.put("supplierName", o.getSupplier().getName());
            map.put("supplierEmail", o.getSupplier().getEmail());
        }
        if (o.getPurchaseRequest() != null) {
            PurchaseRequest pr = o.getPurchaseRequest();
            map.put("requestId", pr.getRequestId());
            map.put("quantity", pr.getQuantity());
            if (pr.getUser() != null) {
                map.put("userName", pr.getUser().getFullName());
                map.put("userEmail", pr.getUser().getEmail());
            }
            if (pr.getDepartment() != null) {
                map.put("departmentName", pr.getDepartment().getDepartmentName());
            }
            if (pr.getProduct() != null) {
                map.put("productName", pr.getProduct().getName());
                map.put("sku", pr.getProduct().getSku());
            }
        }
        return map;
    }
}
