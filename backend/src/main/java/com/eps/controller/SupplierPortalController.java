package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.entity.*;
import com.eps.repository.*;
import com.eps.service.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/supplier")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SupplierPortalController {

    private final RfqService rfqService;
    private final QuotationService quotationService;
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;

    @Data
    public static class ProductCreateRequest {
        private String name;
        private String sku;
        private String description;
        private BigDecimal pricePerProduct;
        private Integer numberOfQuantities;
        private Long categoryId;
        private String categoryName;
        private String imageUrl;
    }

    @Data
    public static class QuoteSubmissionRequest {
        private Long supplierId;
        private BigDecimal quotedPrice;
        private Integer deliveryDays;
        private String comments;
    }

    @Data
    public static class ShipmentUpdateRequest {
        private String trackingNumber;
        private String shipmentDetails;
        private String carrier;
        private String estimatedDelivery;
    }

    @Data
    public static class StatusUpdateRequest {
        private String status;
        private String remarks;
    }

    @Data
    public static class InvoiceSubmissionRequest {
        private String invoiceNumber;
        private BigDecimal amount;
        private String notes;
    }

    /**
     * Helper to resolve the authenticated Supplier entity based on Principal
     */
    private Supplier resolveAuthenticatedSupplier(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) return null;
        String email = principal.getName();
        return supplierRepository.findByEmailIgnoreCase(email)
                .or(() -> supplierRepository.findByNameIgnoreCase(email))
                .or(() -> {
                    // Try looking up via user's full name
                    User user = userService.getUserByEmail(email).orElse(null);
                    if (user != null) {
                        return supplierRepository.findByNameIgnoreCase(user.getFullName());
                    }
                    return Optional.empty();
                })
                .orElse(null);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private Supplier resolveRequestedSupplier(Long requestedSupplierId, Principal principal) {
        if (isAdmin() && requestedSupplierId != null) {
            return supplierRepository.findById(requestedSupplierId).orElse(null);
        }
        return resolveAuthenticatedSupplier(principal);
    }

    private void ensureOrderAccess(PurchaseOrder order, Principal principal) {
        if (isAdmin()) return;
        Supplier supplier = resolveAuthenticatedSupplier(principal);
        if (supplier == null || order.getSupplier() == null ||
                !supplier.getSupplierId().equals(order.getSupplier().getSupplierId())) {
            throw new org.springframework.security.access.AccessDeniedException("Order is not assigned to the authenticated supplier");
        }
    }

    private void ensureProductAccess(Product product, Principal principal) {
        if (isAdmin()) return;
        Supplier supplier = resolveAuthenticatedSupplier(principal);
        if (supplier == null || product.getSupplier() == null ||
                !supplier.getSupplierId().equals(product.getSupplier().getSupplierId())) {
            throw new org.springframework.security.access.AccessDeniedException("Product does not belong to the authenticated supplier");
        }
    }

    /**
     * Real database Dashboard Statistics for the authenticated Supplier
     */
    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> getSupplierDashboardStats(Principal principal) {
        Supplier supplier = resolveAuthenticatedSupplier(principal);
        if (supplier == null) {
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("assignedOrders", 0);
            emptyStats.put("pendingOrders", 0);
            emptyStats.put("acceptedOrders", 0);
            emptyStats.put("ordersProcessing", 0);
            emptyStats.put("shippedOrders", 0);
            emptyStats.put("deliveredOrders", 0);
            emptyStats.put("pendingPayments", 0);
            emptyStats.put("completedPayments", 0);
            emptyStats.put("unreadNotifications", 0);
            emptyStats.put("supplierName", "Unregistered Supplier");
            return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Stats fetched").data(emptyStats).build());
        }

        List<PurchaseOrder> orders = purchaseOrderRepository.findBySupplier(supplier);

        long assignedCount = orders.size();
        long pendingCount = orders.stream()
                .filter(o -> "ASSIGNED".equalsIgnoreCase(o.getStatus()) || "CREATED".equalsIgnoreCase(o.getStatus()))
                .count();
        // PAID means Admin settled payment — transferred to supplier for fulfillment/dispatch.
        long acceptedCount = orders.stream()
                .filter(o -> "ACCEPTED".equalsIgnoreCase(o.getStatus()) || "PAID".equalsIgnoreCase(o.getStatus()) || "ORDERED".equalsIgnoreCase(o.getStatus()))
                .count();
        long processingCount = orders.stream()
                .filter(o -> "PROCESSING".equalsIgnoreCase(o.getStatus()) || "PAID".equalsIgnoreCase(o.getStatus()) || "ORDERED".equalsIgnoreCase(o.getStatus()))
                .count();
        long shippedCount = orders.stream()
                .filter(o -> "SHIPPED".equalsIgnoreCase(o.getStatus()) || "DISPATCHED".equalsIgnoreCase(o.getStatus()))
                .count();
        long deliveredCount = orders.stream()
                .filter(o -> "DELIVERED".equalsIgnoreCase(o.getStatus()) || "COMPLETED".equalsIgnoreCase(o.getStatus()))
                .count();
        long paidReadyCount = orders.stream()
                .filter(o -> "PAID".equalsIgnoreCase(o.getStatus()))
                .count();

        List<Invoice> invoices = invoiceRepository.findBySupplierSupplierId(supplier.getSupplierId());
        long pendingPayments = invoices.stream()
                .filter(i -> !"PAID".equalsIgnoreCase(i.getStatus()))
                .count();
        List<Payment> payments = paymentRepository.findByInvoiceSupplierSupplierId(supplier.getSupplierId());
        long completedPayments = payments.stream()
                .filter(p -> "COMPLETED".equalsIgnoreCase(p.getStatus()))
                .count();

        long unreadNotifications = notificationService.getNotifications(supplier.getEmail(), "SUPPLIER").stream()
                .filter(n -> !Boolean.TRUE.equals(n.getIsRead()))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("supplierId", supplier.getSupplierId());
        stats.put("supplierName", supplier.getName());
        stats.put("assignedOrders", assignedCount);
        stats.put("pendingOrders", pendingCount);
        stats.put("acceptedOrders", acceptedCount);
        stats.put("ordersProcessing", processingCount);
        stats.put("shippedOrders", shippedCount);
        stats.put("deliveredOrders", deliveredCount);
        stats.put("paidReadyOrders", paidReadyCount);
        stats.put("paidReadyCount", paidReadyCount);
        stats.put("pendingPayments", pendingPayments);
        stats.put("completedPayments", completedPayments);
        stats.put("unreadNotifications", unreadNotifications);

        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Supplier dashboard stats fetched").data(stats).build());
    }

    /**
     * Get orders strictly assigned to the authenticated Supplier (Supplier Isolation)
     */
    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<List<PurchaseOrder>> getSupplierOrders(
            @RequestParam(required = false) Long supplierId,
            Principal principal) {
        Supplier supplier = resolveRequestedSupplier(supplierId, principal);

        if (supplier != null) {
            return ResponseEntity.ok(purchaseOrderRepository.findBySupplier(supplier));
        }
        return ResponseEntity.ok(Collections.emptyList());
    }

    /**
     * Accept an assigned order
     */
    @PutMapping("/orders/{id}/accept")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> acceptOrder(@PathVariable Long id, Principal principal) {
        PurchaseOrder order = purchaseOrderService.getOrderById(id);
        ensureOrderAccess(order, principal);
        order.setStatus("ACCEPTED");
        PurchaseOrder saved = purchaseOrderRepository.save(order);

        // Update purchase request status if needed
        if (order.getPurchaseRequest() != null) {
            PurchaseRequest pr = order.getPurchaseRequest();
            pr.setStatus("PROCESSING");
            purchaseRequestRepository.save(pr);

            if (pr.getUser() != null) {
                notificationService.sendNotification(
                        pr.getUser().getEmail(),
                        "USER",
                        "Order Accepted by Supplier",
                        "Supplier has accepted order ORD-" + order.getOrderId() + " for requisition #" + pr.getRequestId() + ". Production/Fulfillment is underway."
                );
            }
        }

        notificationService.sendNotification(
                null,
                "ADMIN",
                "Order ORD-" + order.getOrderId() + " Accepted",
                "Supplier accepted order ORD-" + order.getOrderId() + " for requisition #" + (order.getPurchaseRequest() != null ? order.getPurchaseRequest().getRequestId() : "N/A")
        );

        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Order accepted successfully").data(saved).build());
    }

    /**
     * Decline / Reject an assigned order
     */
    @PutMapping("/orders/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> rejectOrder(
            @PathVariable Long id,
            @RequestBody(required = false) StatusUpdateRequest request,
            Principal principal) {
        PurchaseOrder order = purchaseOrderService.getOrderById(id);
        ensureOrderAccess(order, principal);
        order.setStatus("DECLINED");
        String remarks = (request != null && request.getRemarks() != null) ? request.getRemarks() : "Declined by Supplier";
        order.setShipmentDetails("Declined: " + remarks);
        PurchaseOrder saved = purchaseOrderRepository.save(order);

        notificationService.sendNotification(
                null,
                "ADMIN",
                "Order ORD-" + order.getOrderId() + " Declined by Supplier",
                "Supplier declined Order ORD-" + order.getOrderId() + ". Reason: " + remarks
        );

        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Order declined").data(saved).build());
    }

    /**
     * Update order status to PROCESSING or other stage
     */
    @PutMapping("/orders/{id}/status")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request,
            Principal principal) {
        PurchaseOrder order = purchaseOrderService.getOrderById(id);
        ensureOrderAccess(order, principal);
        if (request != null && request.getStatus() != null) {
            order.setStatus(request.getStatus().toUpperCase());
        }
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Order status updated").data(saved).build());
    }

    /**
     * Update shipment details: Tracking number, carrier, dispatch order
     */
    @PutMapping("/orders/{id}/shipment")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> updateShipment(
            @PathVariable Long id,
            @RequestBody ShipmentUpdateRequest request,
            Principal principal) {
        PurchaseOrder order = purchaseOrderService.getOrderById(id);
        ensureOrderAccess(order, principal);
        if (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
            order.setTrackingNumber(request.getTrackingNumber());
        }
        String carrier = (request.getCarrier() != null && !request.getCarrier().isBlank()) ? request.getCarrier() : "FedEx Priority Freight";
        String details = carrier + (request.getShipmentDetails() != null ? " · " + request.getShipmentDetails() : "");
        order.setShipmentDetails(details);
        order.setStatus("SHIPPED");
        PurchaseOrder saved = purchaseOrderRepository.save(order);

        if (order.getPurchaseRequest() != null) {
            PurchaseRequest pr = order.getPurchaseRequest();
            pr.setStatus("DISPATCHED");
            purchaseRequestRepository.save(pr);

            if (pr.getUser() != null) {
                notificationService.sendNotification(
                        pr.getUser().getEmail(),
                        "USER",
                        "Order Shipped: ORD-" + order.getOrderId(),
                        "Your requisition #" + pr.getRequestId() + " has been dispatched via " + carrier + ". Tracking #: " + order.getTrackingNumber()
                );
            }
        }

        notificationService.sendNotification(
                null,
                "ADMIN",
                "Shipment Dispatched: ORD-" + order.getOrderId(),
                "Order ORD-" + order.getOrderId() + " was shipped via " + carrier + " (Tracking: " + order.getTrackingNumber() + ")"
        );

        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Shipment information updated").data(saved).build());
    }

    /**
     * Mark order as DELIVERED
     */
    @PutMapping("/orders/{id}/deliver")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> deliverOrder(@PathVariable Long id, Principal principal) {
        PurchaseOrder order = purchaseOrderService.getOrderById(id);
        ensureOrderAccess(order, principal);
        order.setStatus("DELIVERED");
        PurchaseOrder saved = purchaseOrderRepository.save(order);

        if (order.getPurchaseRequest() != null) {
            PurchaseRequest pr = order.getPurchaseRequest();
            pr.setStatus("DELIVERED");
            purchaseRequestRepository.save(pr);

            if (pr.getUser() != null) {
                notificationService.sendNotification(
                        pr.getUser().getEmail(),
                        "USER",
                        "Order Delivered: ORD-" + order.getOrderId(),
                        "Your requisition #" + pr.getRequestId() + " (" + (pr.getProduct() != null ? pr.getProduct().getName() : "Item") + ") has been successfully delivered and verified."
                );
            }
        }

        notificationService.sendNotification(
                null,
                "ADMIN",
                "Order Delivered: ORD-" + order.getOrderId(),
                "Order ORD-" + order.getOrderId() + " delivery completed."
        );

        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Order marked as DELIVERED").data(saved).build());
    }

    /**
     * Get payments strictly belonging to the authenticated Supplier
     */
    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> getSupplierPayments(
            @RequestParam(required = false) Long supplierId,
            Principal principal) {
        Supplier supplier = resolveRequestedSupplier(supplierId, principal);

        if (supplier == null) {
            return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Payments fetched").data(Collections.emptyList()).build());
        }

        List<Payment> payments = paymentRepository.findByInvoiceSupplierSupplierId(supplier.getSupplierId());
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
                    }
                }
            }
            return map;
        }).toList();

        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Supplier payments fetched").data(result).build());
    }

    @GetMapping("/rfqs")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN', 'PROCUREMENT')")
    public ResponseEntity<List<Rfq>> getOpenRfqs() {
        return ResponseEntity.ok(rfqService.getOpenRfqs());
    }

    @PostMapping("/rfq/{id}/quote")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<Quotation> submitQuote(@PathVariable Long id, @RequestBody QuoteSubmissionRequest request, Principal principal) {
        Supplier supplier = resolveAuthenticatedSupplier(principal);
        Long supId = (isAdmin() && request.getSupplierId() != null)
            ? request.getSupplierId()
            : (supplier != null ? supplier.getSupplierId() : null);
        if (supId == null) return ResponseEntity.status(403).build();
        Quotation quotation = quotationService.submitQuotation(
                id,
                supId,
                request.getQuotedPrice(),
                request.getDeliveryDays(),
                request.getComments()
        );
        return ResponseEntity.ok(quotation);
    }

    @GetMapping("/quotes")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<List<Quotation>> getSupplierQuotes(@RequestParam(required = false) Long supplierId, Principal principal) {
        Supplier supplier = resolveRequestedSupplier(supplierId, principal);
        if (supplier == null) return ResponseEntity.ok(java.util.Collections.emptyList());
        return ResponseEntity.ok(quotationService.getQuotationsBySupplier(supplier.getSupplierId()));
    }

    @PostMapping("/orders/{id}/invoice")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<Invoice> submitInvoice(@PathVariable Long id, @RequestBody InvoiceSubmissionRequest request, Principal principal) {
        ensureOrderAccess(purchaseOrderService.getOrderById(id), principal);
        Invoice invoice = invoiceService.submitInvoice(id, request.getInvoiceNumber(), request.getAmount(), request.getNotes());
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN', 'FINANCE')")
    public ResponseEntity<List<Invoice>> getInvoices(@RequestParam(required = false) Long supplierId, Principal principal) {
        Supplier supplier = resolveRequestedSupplier(supplierId, principal);
        if (supplier != null) {
            return ResponseEntity.ok(invoiceService.getInvoicesBySupplier(supplier.getSupplierId()));
        }
        if (isAdmin()) return ResponseEntity.ok(invoiceService.getAllInvoices());
        return ResponseEntity.ok(java.util.Collections.emptyList());
    }

    /**
     * Supplier Product Catalog Management: List all products
     */
    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> getSupplierProducts(Principal principal) {
        Supplier supplier = resolveAuthenticatedSupplier(principal);
        List<Product> products = isAdmin() ? productRepository.findAll()
                : (supplier == null ? List.of() : productRepository.findBySupplierSupplierId(supplier.getSupplierId()));
        List<Map<String, Object>> result = products.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("productId", p.getProductId());
            map.put("name", p.getName());
            map.put("sku", p.getSku());
            map.put("description", p.getDescription());
            map.put("pricePerProduct", p.getPricePerProduct());
            map.put("numberOfQuantities", p.getNumberOfQuantities());
            map.put("imageUrl", p.getImageUrl());
            map.put("status", p.getStatus());
            map.put("supplierId", p.getSupplier() != null ? p.getSupplier().getSupplierId() : null);
            map.put("supplierName", p.getSupplier() != null ? p.getSupplier().getName() : null);
            map.put("categoryName", p.getCategory() != null ? p.getCategory().getCategoryName() : "General");
            map.put("categoryId", p.getCategory() != null ? p.getCategory().getCategoryId() : null);
            return map;
        }).toList();
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Catalog products fetched").data(result).build());
    }

    /**
     * Supplier Product Catalog Management: Add new product to catalog
     */
    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> createProduct(@RequestBody ProductCreateRequest req, Principal principal) {
        if (req.getName() == null || req.getName().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponseDto.builder().success(false).message("Product name is mandatory").build());
        }
        if (req.getPricePerProduct() == null || req.getPricePerProduct().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(ApiResponseDto.builder().success(false).message("Valid unit price is mandatory").build());
        }

        Product p = new Product();
        p.setName(req.getName());
        p.setSku(req.getSku() != null && !req.getSku().isBlank() ? req.getSku() : "SKU-VEN-" + (1000 + (int)(Math.random() * 9000)));
        p.setDescription(req.getDescription() != null ? req.getDescription() : req.getName());
        p.setPricePerProduct(req.getPricePerProduct());
        p.setNumberOfQuantities(req.getNumberOfQuantities() != null ? req.getNumberOfQuantities() : 50);
        p.setImageUrl(req.getImageUrl() != null && !req.getImageUrl().isBlank() ? req.getImageUrl() : "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=400");
        p.setStatus(com.eps.enums.ProductStatus.ACTIVE);

        if (req.getCategoryId() != null) {
            categoryRepository.findById(req.getCategoryId()).ifPresent(p::setCategory);
        } else if (req.getCategoryName() != null && !req.getCategoryName().isBlank()) {
            Category cat = categoryRepository.findByCategoryName(req.getCategoryName())
                    .orElseGet(() -> categoryRepository.save(new Category(null, req.getCategoryName())));
            p.setCategory(cat);
        } else {
            categoryRepository.findAll().stream().findFirst().ifPresent(p::setCategory);
        }

        departmentRepository.findAll().stream().findFirst().ifPresent(p::setDepartment);
        if (principal != null) {
            userService.getUserByEmail(principal.getName()).ifPresent(p::setUser);
        }
        Supplier authenticatedSupplier = resolveAuthenticatedSupplier(principal);
        if (authenticatedSupplier == null && !isAdmin()) {
            return ResponseEntity.status(403).body(ApiResponseDto.builder()
                    .success(false).message("Authenticated supplier profile not found").build());
        }
        p.setSupplier(authenticatedSupplier);

        Product saved = productRepository.save(p);
        log.info("Supplier created new catalog product: ID {}, Name: {}", saved.getProductId(), saved.getName());

        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Product successfully added to enterprise catalog!").data(saved).build());
    }

    /**
     * Supplier Product Catalog Management: Update price, stock, or details
     */
    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> deleteProduct(@PathVariable Long id, Principal principal) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID " + id));
        ensureProductAccess(p, principal);
        productRepository.delete(p);
        log.info("Supplier deleted catalog product: ID {}", id);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Product deleted/deactivated successfully!").build());
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> updateProduct(@PathVariable Long id, @RequestBody ProductCreateRequest req, Principal principal) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID " + id));
        ensureProductAccess(p, principal);

        if (req.getName() != null && !req.getName().isBlank()) p.setName(req.getName());
        if (req.getPricePerProduct() != null && req.getPricePerProduct().compareTo(BigDecimal.ZERO) > 0) p.setPricePerProduct(req.getPricePerProduct());
        if (req.getNumberOfQuantities() != null && req.getNumberOfQuantities() >= 0) p.setNumberOfQuantities(req.getNumberOfQuantities());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getImageUrl() != null && !req.getImageUrl().isBlank()) p.setImageUrl(req.getImageUrl());
        if (req.getSku() != null && !req.getSku().isBlank()) p.setSku(req.getSku());

        Product saved = productRepository.save(p);
        log.info("Supplier updated catalog product: ID {}, New Price: {}, Stock: {}", saved.getProductId(), saved.getPricePerProduct(), saved.getNumberOfQuantities());

        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Product details & pricing updated!").data(saved).build());
    }
}
