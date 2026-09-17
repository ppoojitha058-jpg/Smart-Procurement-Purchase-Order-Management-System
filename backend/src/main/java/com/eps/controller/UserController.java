package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.entity.Notification;
import com.eps.entity.PurchaseOrder;
import com.eps.entity.PurchaseRequest;
import com.eps.service.NotificationService;
import com.eps.service.PurchaseOrderService;
import com.eps.service.PurchaseRequestService;
import com.eps.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User Controller
 * Handles user-specific APIs with strict database-driven stats and user data isolation.
 * Base URL: /api/users
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final PurchaseRequestService purchaseRequestService;
    private final PurchaseOrderService purchaseOrderService;
    private final NotificationService notificationService;

    /**
     * User Dashboard Overview
     * Computes real-time statistics from database for the authenticated user.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseDto> userDashboard(Principal principal) {
        String email = principal != null ? principal.getName() : "employee@eps.com";
        log.info("User Dashboard accessed for user: {}", email);

        List<PurchaseRequest> myReqs = purchaseRequestService.getRequestsByUser(email);
        List<PurchaseOrder> myOrders = purchaseOrderService.getOrdersByUser(email);
        List<Notification> myNotifs = notificationService.getNotifications(email, "USER");

        long totalRequests = myReqs.size();
        long pendingRequests = myReqs.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();
        long approvedRequests = myReqs.stream().filter(r -> isApprovedLifecycle(r.getStatus())).count();
        long rejectedRequests = myReqs.stream().filter(r -> "REJECTED".equalsIgnoreCase(r.getStatus())).count();
        long ordersCount = myOrders.size();
        long unreadNotifs = myNotifs.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();
        double spentYtd = myReqs.stream()
                .filter(r -> isApprovedLifecycle(r.getStatus()))
                .mapToDouble(r -> r.getTotalPrice() != null ? r.getTotalPrice().doubleValue() : 0.0)
                .sum();

        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("dashboardName", "User Dashboard");
        dashboardData.put("accountStatus", "Active");
        dashboardData.put("recentActivity", totalRequests > 0 ? ("Requisition #" + myReqs.get(myReqs.size() - 1).getRequestId() + " (" + myReqs.get(myReqs.size() - 1).getStatus() + ")") : "No recent activity");
        dashboardData.put("lastLogin", System.currentTimeMillis());
        dashboardData.put("totalRequests", totalRequests);
        dashboardData.put("pendingRequests", pendingRequests);
        dashboardData.put("approvedRequests", approvedRequests);
        dashboardData.put("rejectedRequests", rejectedRequests);
        dashboardData.put("ordersCount", ordersCount);
        dashboardData.put("unreadNotifications", unreadNotifs);
        dashboardData.put("spentYtd", spentYtd);

        ApiResponseDto response = ApiResponseDto.builder()
                .success(true)
                .message("User Dashboard loaded successfully")
                .data(dashboardData)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(Principal principal) {
        String email = principal != null ? principal.getName() : "employee@eps.com";
        List<PurchaseRequest> myReqs = purchaseRequestService.getRequestsByUser(email);
        List<PurchaseOrder> myOrders = purchaseOrderService.getOrdersByUser(email);
        List<Notification> myNotifs = notificationService.getNotifications(email, "USER");

        long totalRequests = myReqs.size();
        long pendingRequests = myReqs.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();
        long approvedRequests = myReqs.stream().filter(r -> isApprovedLifecycle(r.getStatus())).count();
        long rejectedRequests = myReqs.stream().filter(r -> "REJECTED".equalsIgnoreCase(r.getStatus())).count();
        double spentYtd = myReqs.stream()
                .filter(r -> isApprovedLifecycle(r.getStatus()))
                .mapToDouble(r -> r.getTotalPrice() != null ? r.getTotalPrice().doubleValue() : 0.0)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalRequests);
        stats.put("pending", pendingRequests);
        stats.put("approved", approvedRequests);
        stats.put("rejected", rejectedRequests);
        stats.put("orders", myOrders.size());
        stats.put("unreadNotifications", myNotifs.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count());
        stats.put("spentYtd", spentYtd);

        return ResponseEntity.ok(stats);
    }

    private boolean isApprovedLifecycle(String status) {
        if (status == null) return false;
        return switch (status.toUpperCase()) {
            case "APPROVED", "PO_ISSUED", "ORDERED", "PROCESSING", "ACCEPTED", "SHIPPED", "DISPATCHED", "PAID", "DELIVERED", "COMPLETED" -> true;
            default -> false;
        };
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponseDto> getProfile(Principal principal) {
        String email = principal != null ? principal.getName() : "employee@eps.com";
        log.info("Profile request received for user: {}", email);

        return userService.getUserByEmail(email)
                .map(user -> {
                    Map<String, Object> profileData = new HashMap<>();
                    profileData.put("userId", user.getUserId());
                    profileData.put("fullName", user.getFullName());
                    profileData.put("email", user.getEmail());
                    profileData.put("phone", user.getPhone());
                    profileData.put("address", user.getAddress());
                    profileData.put("enabled", user.getEnabled());
                    profileData.put("role", user.getRole().getRoleName());
                    if (user.getDepartment() != null) {
                        profileData.put("departmentId", user.getDepartment().getDepartmentId());
                        profileData.put("departmentName", user.getDepartment().getDepartmentName());
                    }

                    ApiResponseDto response = ApiResponseDto.builder()
                            .success(true)
                            .message("User profile loaded successfully")
                            .data(profileData)
                            .build();

                    return new ResponseEntity<>(response, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    ApiResponseDto response = ApiResponseDto.builder()
                            .success(false)
                            .message("User profile not found")
                            .error("USER_NOT_FOUND")
                            .build();
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                });
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<PurchaseRequest>> getMyRequests(Principal principal) {
        String email = principal != null ? principal.getName() : "employee@eps.com";
        return ResponseEntity.ok(purchaseRequestService.getRequestsByUser(email));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<PurchaseOrder>> getMyOrders(Principal principal) {
        String email = principal != null ? principal.getName() : "employee@eps.com";
        return ResponseEntity.ok(purchaseOrderService.getOrdersByUser(email));
    }

    @GetMapping("/order-tracking/{orderId}")
    public ResponseEntity<PurchaseOrder> trackOrder(@PathVariable Long orderId, Principal principal, org.springframework.security.core.Authentication authentication) {
        PurchaseOrder po = purchaseOrderService.getOrderById(orderId);
        if (principal != null && po.getPurchaseRequest() != null && po.getPurchaseRequest().getUser() != null) {
            String currentUser = principal.getName();
            boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MANAGER".equals(a.getAuthority()));
            if (!po.getPurchaseRequest().getUser().getEmail().equalsIgnoreCase(currentUser) && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(po);
    }
}
