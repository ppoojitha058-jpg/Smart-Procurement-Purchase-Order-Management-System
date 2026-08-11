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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    @PostMapping
    public ResponseEntity<ApiResponseDto> createPurchaseRequest(@RequestBody PurchaseRequestDto dto) {
        PurchaseRequest request = buildPurchaseRequest(dto);
        PurchaseRequest created = purchaseRequestService.createPurchaseRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.builder().success(true).message("Purchase request created").data(toDto(created)).build());
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto> getAllPurchaseRequests() {
        List<PurchaseRequest> requests = purchaseRequestService.getAllPurchaseRequests();
        List<PurchaseRequestDto> dtos = requests.stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Purchase requests fetched").data(dtos).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto> getPurchaseRequestById(@PathVariable Long id) {
        PurchaseRequest request = purchaseRequestService.getPurchaseRequestById(id);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Purchase request fetched").data(toDto(request)).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto> updatePurchaseRequest(@PathVariable Long id, @RequestBody PurchaseRequestDto dto) {
        PurchaseRequest request = buildPurchaseRequest(dto);
        PurchaseRequest updated = purchaseRequestService.updatePurchaseRequest(id, request);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Purchase request updated").data(toDto(updated)).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto> deletePurchaseRequest(@PathVariable Long id) {
        purchaseRequestService.deletePurchaseRequest(id);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Purchase request deleted").build());
    }

    private PurchaseRequest buildPurchaseRequest(PurchaseRequestDto dto) {
        if (dto.getUserId() == null) {
            throw new RuntimeException("User ID is required");
        }
        if (dto.getProductId() == null) {
            throw new RuntimeException("Product ID is required");
        }
        if (dto.getDepartmentId() == null) {
            throw new RuntimeException("Department ID is required");
        }
        if (dto.getQuantity() == null) {
            throw new RuntimeException("Quantity is required");
        }

        User user = userService.getUserById(dto.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productService.getProductById(dto.getProductId());
        Department department = departmentService.getDepartmentById(dto.getDepartmentId());

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

    private PurchaseRequestDto toDto(PurchaseRequest request) {
        PurchaseRequestDto dto = new PurchaseRequestDto();
        dto.setRequestId(request.getRequestId());
        dto.setUserId(request.getUser() != null ? request.getUser().getUserId() : null);
        dto.setProductId(request.getProduct() != null ? request.getProduct().getProductId() : null);
        dto.setDepartmentId(request.getDepartment() != null ? request.getDepartment().getDepartmentId() : null);
        dto.setQuantity(request.getQuantity());
        dto.setTotalPrice(request.getTotalPrice());
        dto.setStatus(request.getStatus());
        return dto;
    }
}
