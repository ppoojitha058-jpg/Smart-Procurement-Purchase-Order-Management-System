package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.dto.ManagerActionDto;
import com.eps.entity.Department;
import com.eps.entity.PurchaseOrder;
import com.eps.entity.PurchaseRequest;
import com.eps.entity.User;
import com.eps.service.DepartmentService;
import com.eps.service.PurchaseOrderService;
import com.eps.service.PurchaseRequestService;
import com.eps.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@CrossOrigin(origins = "*")
public class ManagerController {

    private final PurchaseRequestService purchaseRequestService;
    private final PurchaseOrderService purchaseOrderService;
    private final DepartmentService departmentService;
    private final UserService userService;

    private User resolveManager(Principal principal) {
        return principal == null ? null : userService.getUserByEmail(principal.getName()).orElse(null);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private List<PurchaseRequest> scopedRequests(Principal principal) {
        User manager = resolveManager(principal);
        if (isAdmin()) return purchaseRequestService.getAllPurchaseRequests();
        if (manager == null || manager.getDepartment() == null) return List.of();
        return purchaseRequestService.getRequestsByDepartment(manager.getDepartment().getDepartmentId());
    }

    private boolean canManageRequest(Long requestId, Principal principal) {
        if (isAdmin()) return true;
        User manager = resolveManager(principal);
        PurchaseRequest request = purchaseRequestService.getPurchaseRequestById(requestId);
        return manager != null && manager.getDepartment() != null && request.getDepartment() != null
                && manager.getDepartment().getDepartmentId().equals(request.getDepartment().getDepartmentId());
    }

    @GetMapping("/pending-requests")
    public ResponseEntity<ApiResponseDto> getPendingRequests(Principal principal) {
        String managerEmail = principal != null ? principal.getName() : "manager@eps.com";
        List<PurchaseRequest> pending = scopedRequests(principal).stream()
                .filter(request -> "PENDING".equalsIgnoreCase(request.getStatus())).toList();
        List<Map<String, Object>> result = pending.stream().map(this::formatRequestMap).toList();
        ApiResponseDto response = ApiResponseDto.builder().success(true).message("Pending requests fetched").data(result).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/department-requests")
    public ResponseEntity<ApiResponseDto> getDepartmentRequests(
            @RequestParam(required = false) Long departmentId,
            Principal principal) {
        User manager = resolveManager(principal);
        List<PurchaseRequest> list = isAdmin() && departmentId != null
                ? purchaseRequestService.getRequestsByDepartment(departmentId)
                : scopedRequests(principal);
        List<Map<String, Object>> result = list.stream().map(this::formatRequestMap).toList();
        ApiResponseDto response = ApiResponseDto.builder().success(true).message("Department requests fetched").data(result).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/approval-history")
    public ResponseEntity<ApiResponseDto> getApprovalHistory(Principal principal) {
        List<PurchaseRequest> history = scopedRequests(principal).stream()
            .filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus()) || "REJECTED".equalsIgnoreCase(r.getStatus()) || "PO_ISSUED".equalsIgnoreCase(r.getStatus()) || "DISPATCHED".equalsIgnoreCase(r.getStatus()) || "DELIVERED".equalsIgnoreCase(r.getStatus()))
            .toList();
        List<Map<String, Object>> result = history.stream().map(this::formatRequestMap).toList();
        ApiResponseDto response = ApiResponseDto.builder().success(true).message("Approval history fetched").data(result).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Map<String, Object> formatRequestMap(PurchaseRequest r) {
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", r.getRequestId());
        map.put("quantity", r.getQuantity());
        map.put("totalPrice", r.getTotalPrice());
        map.put("status", r.getStatus());
        map.put("approvedBy", r.getApprovedBy());
        map.put("rejectedBy", r.getRejectedBy());
        map.put("approvalDate", r.getApprovalDate());
        map.put("rejectionDate", r.getRejectionDate());
        map.put("managerComments", r.getManagerComments());
        map.put("createdDate", r.getCreatedDate());
        if (r.getUser() != null) {
            map.put("userName", r.getUser().getFullName());
            map.put("userEmail", r.getUser().getEmail());
            Map<String, Object> userObj = new HashMap<>();
            userObj.put("userId", r.getUser().getUserId());
            userObj.put("fullName", r.getUser().getFullName());
            userObj.put("email", r.getUser().getEmail());
            map.put("user", userObj);
        }
        if (r.getProduct() != null) {
            map.put("productName", r.getProduct().getName());
            map.put("sku", r.getProduct().getSku());
            map.put("pricePerProduct", r.getProduct().getPricePerProduct());
            Map<String, Object> prodObj = new HashMap<>();
            prodObj.put("productId", r.getProduct().getProductId());
            prodObj.put("name", r.getProduct().getName());
            prodObj.put("sku", r.getProduct().getSku());
            prodObj.put("pricePerProduct", r.getProduct().getPricePerProduct());
            map.put("product", prodObj);
        }
        if (r.getDepartment() != null) {
            map.put("departmentName", r.getDepartment().getDepartmentName());
            Map<String, Object> deptObj = new HashMap<>();
            deptObj.put("departmentId", r.getDepartment().getDepartmentId());
            deptObj.put("departmentName", r.getDepartment().getDepartmentName());
            map.put("department", deptObj);
        }
        return map;
    }

    @GetMapping("/department-orders")
    public ResponseEntity<List<PurchaseOrder>> getDepartmentOrders(@RequestParam(required = false) Long departmentId, Principal principal) {
        if (departmentId != null && isAdmin()) {
            return ResponseEntity.ok(purchaseOrderService.getOrdersByDepartment(departmentId));
        }
        User manager = resolveManager(principal);
        if (manager != null && manager.getDepartment() != null) {
            return ResponseEntity.ok(purchaseOrderService.getOrdersByDepartment(manager.getDepartment().getDepartmentId()));
        }
        return ResponseEntity.ok(isAdmin() ? purchaseOrderService.getAllOrders() : List.of());
    }

    @GetMapping("/my-requests")
    public ResponseEntity<ApiResponseDto> getMyRequests(Principal principal) {
        String managerEmail = principal != null ? principal.getName() : "manager@eps.com";
        List<PurchaseRequest> list = purchaseRequestService.getRequestsByManager(managerEmail);
        ApiResponseDto response = ApiResponseDto.builder().success(true).message("Manager requests fetched").data(list).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/my-pending")
    public ResponseEntity<ApiResponseDto> getMyPendingRequests(Principal principal) {
        return getPendingRequests(principal);
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponseDto> getDashboardStats(Principal principal) {
        String managerEmail = principal != null ? principal.getName() : "manager@eps.com";
        User manager = userService.getUserByEmail(managerEmail).orElse(null);
        List<PurchaseRequest> scopedRequests = scopedRequests(principal);

        List<PurchaseRequest> pending = scopedRequests.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).toList();
        List<PurchaseRequest> approved = scopedRequests.stream().filter(r -> isApprovedLifecycle(r.getStatus())).toList();
        List<PurchaseRequest> rejected = scopedRequests.stream().filter(r -> "REJECTED".equalsIgnoreCase(r.getStatus())).toList();
        List<PurchaseRequest> dispatched = scopedRequests.stream().filter(r -> "DISPATCHED".equalsIgnoreCase(r.getStatus()) || "SHIPPED".equalsIgnoreCase(r.getStatus()) || "PROCESSING".equalsIgnoreCase(r.getStatus()) || "PO_ISSUED".equalsIgnoreCase(r.getStatus()) || "ORDERED".equalsIgnoreCase(r.getStatus())).toList();
        List<PurchaseRequest> delivered = scopedRequests.stream().filter(r -> "DELIVERED".equalsIgnoreCase(r.getStatus()) || "COMPLETED".equalsIgnoreCase(r.getStatus())).toList();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", scopedRequests.size());
        stats.put("pendingApproval", pending.size());
        stats.put("approved", approved.size());
        stats.put("rejected", rejected.size());
        stats.put("ordersInProgress", dispatched.size());
        stats.put("completedOrders", delivered.size());
        stats.put("departmentName", (manager != null && manager.getDepartment() != null) ? manager.getDepartment().getDepartmentName() : "Enterprise-Wide");

        double totalRequested = scopedRequests.stream()
                .mapToDouble(r -> r.getTotalPrice() != null ? r.getTotalPrice().doubleValue() : 0.0)
                .sum();
        double pendingAmount = pending.stream()
                .mapToDouble(r -> r.getTotalPrice() != null ? r.getTotalPrice().doubleValue() : 0.0)
                .sum();
        double approvedAmount = approved.stream()
                .mapToDouble(r -> r.getTotalPrice() != null ? r.getTotalPrice().doubleValue() : 0.0)
                .sum();
        double rejectedAmount = rejected.stream()
                .mapToDouble(r -> r.getTotalPrice() != null ? r.getTotalPrice().doubleValue() : 0.0)
                .sum();

        stats.put("totalRequestedAmount", totalRequested);
        stats.put("pendingAmount", pendingAmount);
        stats.put("approvedAmount", approvedAmount);
        stats.put("rejectedAmount", rejectedAmount);

        ApiResponseDto response = ApiResponseDto.builder().success(true).message("Dashboard stats fetched").data(stats).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private boolean isApprovedLifecycle(String status) {
        if (status == null) return false;
        return switch (status.toUpperCase()) {
            case "APPROVED", "PO_ISSUED", "ORDERED", "PROCESSING", "ACCEPTED", "SHIPPED", "DISPATCHED", "PAID", "DELIVERED", "COMPLETED" -> true;
            default -> false;
        };
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<ApiResponseDto> approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ManagerActionDto dto,
            Principal principal) {
        if (!canManageRequest(id, principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponseDto.builder()
                    .success(false).message("Request is outside the manager's authorized department").build());
        }
        String managerEmail = principal != null ? principal.getName() : "manager@eps.com";
        String remarks = (dto != null && dto.getRemarks() != null && !dto.getRemarks().isBlank()) ? dto.getRemarks() : "Approved by Manager";
        PurchaseRequest updated = purchaseRequestService.approveRequest(id, managerEmail, remarks);
        ApiResponseDto response = ApiResponseDto.builder().success(true).message("Request approved successfully").data(updated).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<ApiResponseDto> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ManagerActionDto dto,
            Principal principal) {
        if (!canManageRequest(id, principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponseDto.builder()
                    .success(false).message("Request is outside the manager's authorized department").build());
        }
        String managerEmail = principal != null ? principal.getName() : "manager@eps.com";
        String remarks = (dto != null && dto.getRemarks() != null && !dto.getRemarks().isBlank()) ? dto.getRemarks() : "Rejected by Manager: Policy or budget limit";
        PurchaseRequest updated = purchaseRequestService.rejectRequest(id, managerEmail, remarks);
        ApiResponseDto response = ApiResponseDto.builder().success(true).message("Request rejected successfully").data(updated).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
