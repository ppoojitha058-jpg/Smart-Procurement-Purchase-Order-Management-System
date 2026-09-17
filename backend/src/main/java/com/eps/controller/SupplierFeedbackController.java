package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.entity.PurchaseOrder;
import com.eps.entity.Supplier;
import com.eps.entity.SupplierFeedback;
import com.eps.entity.User;
import com.eps.repository.PurchaseOrderRepository;
import com.eps.repository.SupplierFeedbackRepository;
import com.eps.repository.SupplierRepository;
import com.eps.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplierFeedbackController {
    private final SupplierFeedbackRepository feedbackRepository;
    private final PurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final UserService userService;

    @Data
    public static class FeedbackRequest {
        private Integer rating;
        private Integer qualityRating;
        private Integer speedRating;
        private String comments;
    }

    @PostMapping("/api/users/requests/{requestId}/feedback")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponseDto> submitFeedback(@PathVariable Long requestId,
                                                          @RequestBody FeedbackRequest request,
                                                          Principal principal) {
        PurchaseOrder order = orderRepository.findByPurchaseRequestRequestId(requestId).orElse(null);
        return processOrderFeedback(order, request, principal);
    }

    @PostMapping("/api/users/orders/{orderId}/feedback")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponseDto> submitOrderFeedback(@PathVariable Long orderId,
                                                               @RequestBody FeedbackRequest request,
                                                               Principal principal) {
        PurchaseOrder order = orderRepository.findById(orderId).orElse(null);
        return processOrderFeedback(order, request, principal);
    }

    private ResponseEntity<ApiResponseDto> processOrderFeedback(PurchaseOrder order,
                                                                 FeedbackRequest request,
                                                                 Principal principal) {
        User user = userService.getUserByEmail(principal.getName()).orElse(null);
        if (user == null || order == null || order.getPurchaseRequest() == null || order.getPurchaseRequest().getUser() == null
                || !principal.getName().equalsIgnoreCase(order.getPurchaseRequest().getUser().getEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponseDto.builder().success(false).message("Order is not owned by this user").build());
        }
        if (!"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponseDto.builder().success(false).message("Feedback is available after delivery").build());
        }

        if (request.getQualityRating() == null && request.getRating() != null) {
            request.setQualityRating(request.getRating());
        }
        if (request.getSpeedRating() == null && request.getRating() != null) {
            request.setSpeedRating(request.getRating());
        }
        if (request.getSpeedRating() == null && request.getQualityRating() != null) {
            request.setSpeedRating(request.getQualityRating());
        }

        if (request.getQualityRating() == null || request.getSpeedRating() == null
                || request.getQualityRating() < 1 || request.getQualityRating() > 5
                || request.getSpeedRating() < 1 || request.getSpeedRating() > 5) {
            return ResponseEntity.badRequest().body(ApiResponseDto.builder().success(false).message("Ratings must be between 1 and 5").build());
        }

        SupplierFeedback feedback = feedbackRepository.findByOrderOrderId(order.getOrderId()).orElseGet(SupplierFeedback::new);
        feedback.setOrder(order);
        feedback.setUser(user);
        feedback.setSupplier(order.getSupplier());
        feedback.setProduct(order.getPurchaseRequest() != null ? order.getPurchaseRequest().getProduct() : null);
        feedback.setQualityRating(request.getQualityRating());
        feedback.setSpeedRating(request.getSpeedRating());
        feedback.setComments(request.getComments());
        SupplierFeedback saved = feedbackRepository.save(feedback);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Feedback saved").data(toMap(saved)).build());
    }

    @GetMapping("/api/users/requests/{requestId}/feedback")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserFeedback(@PathVariable Long requestId, Principal principal) {
        return orderRepository.findByPurchaseRequestRequestId(requestId)
                .flatMap(order -> feedbackRepository.findByOrderOrderId(order.getOrderId()))
                .filter(f -> f.getUser() != null && principal.getName().equalsIgnoreCase(f.getUser().getEmail()))
                .map(f -> ResponseEntity.ok(toMap(f)))
                .orElseGet(() -> ResponseEntity.ok(Collections.emptyMap()));
    }

    @GetMapping("/api/users/orders/{orderId}/feedback")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserFeedbackByOrderId(@PathVariable Long orderId, Principal principal) {
        return feedbackRepository.findByOrderOrderId(orderId)
                .filter(f -> f.getUser() != null && principal.getName().equalsIgnoreCase(f.getUser().getEmail()))
                .map(f -> ResponseEntity.ok(toMap(f)))
                .orElseGet(() -> ResponseEntity.ok(Collections.emptyMap()));
    }

    @GetMapping("/api/users/feedback")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Map<String, Object>>> getMyFeedback(Principal principal) {
        return ResponseEntity.ok(feedbackRepository.findByUserEmailOrderByCreatedDateDesc(principal.getName())
                .stream().map(this::toMap).toList());
    }

    @GetMapping("/api/supplier/feedback")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getSupplierFeedback(@RequestParam(required = false) Long supplierId,
                                                                           Principal principal) {
        Supplier supplier = supplierId != null && hasRole("ROLE_ADMIN")
                ? supplierRepository.findById(supplierId).orElse(null)
                : supplierRepository.findByEmailIgnoreCase(principal.getName()).orElse(null);
        if (supplier == null) return ResponseEntity.ok(Collections.emptyList());
        return ResponseEntity.ok(feedbackRepository.findBySupplierSupplierIdOrderByCreatedDateDesc(supplier.getSupplierId())
                .stream().map(this::toMap).toList());
    }

    private boolean hasRole(String role) {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    private Map<String, Object> toMap(SupplierFeedback feedback) {
        Map<String, Object> map = new HashMap<>();
        map.put("feedbackId", feedback.getFeedbackId());
        map.put("orderId", feedback.getOrder() != null ? feedback.getOrder().getOrderId() : null);
        map.put("requestId", feedback.getOrder() != null && feedback.getOrder().getPurchaseRequest() != null ? feedback.getOrder().getPurchaseRequest().getRequestId() : null);
        map.put("productId", feedback.getProduct() != null ? feedback.getProduct().getProductId() : (feedback.getOrder() != null && feedback.getOrder().getPurchaseRequest() != null && feedback.getOrder().getPurchaseRequest().getProduct() != null ? feedback.getOrder().getPurchaseRequest().getProduct().getProductId() : null));
        map.put("productName", feedback.getProduct() != null ? feedback.getProduct().getName() : (feedback.getOrder() != null && feedback.getOrder().getPurchaseRequest() != null && feedback.getOrder().getPurchaseRequest().getProduct() != null ? feedback.getOrder().getPurchaseRequest().getProduct().getName() : "Product"));
        map.put("supplierId", feedback.getSupplier() != null ? feedback.getSupplier().getSupplierId() : null);
        map.put("supplierName", feedback.getSupplier() != null ? feedback.getSupplier().getName() : null);
        map.put("userId", feedback.getUser() != null ? feedback.getUser().getUserId() : null);
        map.put("userName", feedback.getUser() != null ? feedback.getUser().getFullName() : "User");
        map.put("qualityRating", feedback.getQualityRating());
        map.put("speedRating", feedback.getSpeedRating());
        map.put("rating", feedback.getQualityRating());
        double avg = (feedback.getQualityRating() != null && feedback.getSpeedRating() != null)
                ? ((feedback.getQualityRating() + feedback.getSpeedRating()) / 2.0)
                : (feedback.getQualityRating() != null ? feedback.getQualityRating().doubleValue() : 5.0);
        map.put("averageRating", avg);
        map.put("comments", feedback.getComments());
        map.put("createdDate", feedback.getCreatedDate());
        return map;
    }
}
