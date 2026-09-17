package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.dto.PurchaseRequestDto;
import com.eps.entity.Department;
import com.eps.entity.Product;
import com.eps.entity.PurchaseRequest;
import com.eps.entity.User;
import com.eps.service.DepartmentService;
import com.eps.service.ProductService;
import com.eps.service.PurchaseRequestService;
import com.eps.service.UserService;
import com.eps.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/purchase-requests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;
    private final UserService userService;
    private final ProductService productService;
    private final DepartmentService departmentService;
    private final PurchaseOrderRepository purchaseOrderRepository;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponseDto> createPurchaseRequest(@RequestBody PurchaseRequestDto dto, Principal principal) {
        PurchaseRequest request = buildPurchaseRequest(dto, principal);
        PurchaseRequest created = purchaseRequestService.createPurchaseRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.builder().success(true).message("Purchase request created").data(toDto(created)).build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> getAllPurchaseRequests(Authentication authentication) {
        List<PurchaseRequest> requests;
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            requests = purchaseRequestService.getAllPurchaseRequests();
        } else {
            requests = purchaseRequestService.getRequestsByUser(authentication.getName());
        }
        List<PurchaseRequestDto> dtos = requests.stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Purchase requests fetched").data(dtos).build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto> getPurchaseRequestById(@PathVariable Long id, Authentication authentication) {
        PurchaseRequest request = purchaseRequestService.getPurchaseRequestById(id);
        // Ensure USER role can only access their own request
        if (!isAdmin(authentication)) {
            if (request.getUser() != null && !request.getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponseDto.builder().success(false).message("Unauthorized to access this requisition").build());
            }
        }
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Purchase request fetched").data(toDto(request)).build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponseDto> updatePurchaseRequest(@PathVariable Long id, @RequestBody PurchaseRequestDto dto, Principal principal, Authentication authentication) {
        PurchaseRequest existing = purchaseRequestService.getPurchaseRequestById(id);
        ensureEligibleOwner(existing, authentication);
        PurchaseRequest request = buildPurchaseRequest(dto, principal);
        PurchaseRequest updated = purchaseRequestService.updatePurchaseRequest(id, request);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Purchase request updated").data(toDto(updated)).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponseDto> deletePurchaseRequest(@PathVariable Long id, Authentication authentication) {
        PurchaseRequest request = purchaseRequestService.getPurchaseRequestById(id);
        ensureEligibleOwner(request, authentication);
        purchaseRequestService.deletePurchaseRequest(id);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Purchase request deleted").build());
    }

    private PurchaseRequest buildPurchaseRequest(PurchaseRequestDto dto, Principal principal) {
        if (dto.getProductId() == null) {
            throw new RuntimeException("Product ID is required");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        if (principal == null || principal.getName() == null) {
            throw new RuntimeException("Authenticated user identity is required");
        }
        User user = userService.getUserByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User identity could not be resolved"));

        Product product = productService.getProductById(dto.getProductId());

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentService.getDepartmentById(dto.getDepartmentId());
        } else if (user.getDepartment() != null) {
            department = user.getDepartment();
        } else if (product.getDepartment() != null) {
            department = product.getDepartment();
        } else {
            department = departmentService.getAllDepartments().stream().findFirst().orElse(null);
        }

        PurchaseRequest request = new PurchaseRequest();
        request.setUser(user);
        request.setProduct(product);
        request.setDepartment(department);
        request.setQuantity(dto.getQuantity());
        request.setStatus(dto.getStatus() == null || dto.getStatus().isBlank() ? "PENDING" : dto.getStatus());

        if (product.getPricePerProduct() != null) {
            BigDecimal totalPrice = product.getPricePerProduct().multiply(BigDecimal.valueOf(dto.getQuantity()));
            request.setTotalPrice(totalPrice);
        }

        return request;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private void ensureEligibleOwner(PurchaseRequest request, Authentication authentication) {
        if (request.getUser() == null || authentication == null ||
                !request.getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("You can modify only your own purchase requests");
        }
        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending purchase requests can be changed or cancelled");
        }
    }

    private PurchaseRequestDto toDto(PurchaseRequest request) {
        PurchaseRequestDto dto = new PurchaseRequestDto();
        dto.setRequestId(request.getRequestId());
        dto.setUserId(request.getUser() != null ? request.getUser().getUserId() : null);
        dto.setProductId(request.getProduct() != null ? request.getProduct().getProductId() : null);
        dto.setProductName(request.getProduct() != null ? request.getProduct().getName() : "N/A");
        dto.setDepartmentId(request.getDepartment() != null ? request.getDepartment().getDepartmentId() : null);
        dto.setDepartmentName(request.getDepartment() != null ? request.getDepartment().getDepartmentName() : "N/A");
        dto.setQuantity(request.getQuantity());
        dto.setTotalPrice(request.getTotalPrice());
        dto.setStatus(request.getStatus());
        // Manager approval fields from entity
        dto.setApprovedBy(request.getApprovedBy());
        dto.setRejectedBy(request.getRejectedBy());
        if (request.getApprovalDate() != null) {
            dto.setApprovalDate(request.getApprovalDate().toString());
        }
        if (request.getRejectionDate() != null) {
            dto.setRejectionDate(request.getRejectionDate().toString());
        }
        dto.setManagerComments(request.getManagerComments());
        // Requester name
        dto.setRequesterName(request.getUser() != null ? request.getUser().getFullName() : "N/A");
        // Catalog enrichment
        if (request.getProduct() != null) {
            dto.setUnitPrice(request.getProduct().getPricePerProduct());
            dto.setSku(request.getProduct().getSku());
            dto.setImageUrl(request.getProduct().getImageUrl());
            if (request.getProduct().getCategory() != null) {
                dto.setCategoryName(request.getProduct().getCategory().getCategoryName());
            }
        }
        // Dates
        if (request.getCreatedDate() != null) {
            dto.setCreatedDate(request.getCreatedDate().toString());
        }
        // PO and tracking values are returned only when an actual order exists.
        String status = request.getStatus() != null ? request.getStatus() : "PENDING";
        switch (status) {
            case "APPROVED", "PO_ISSUED", "PROCESSING", "DISPATCHED", "DELIVERED", "ORDERED" -> dto.setManagerApproval("Approved by Manager");
            case "REJECTED" -> dto.setManagerApproval("Rejected by Manager");
            default -> dto.setManagerApproval("Awaiting Manager Review");
        }
        purchaseOrderRepository.findByPurchaseRequestRequestId(request.getRequestId()).ifPresent(order -> {
            dto.setPoNumber("PO-" + order.getOrderId());
            dto.setTrackingNumber(order.getTrackingNumber());
            dto.setCarrier(order.getShipmentDetails());
        });
        return dto;
    }
}
