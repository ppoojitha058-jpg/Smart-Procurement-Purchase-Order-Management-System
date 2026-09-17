package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.dto.SupplierDto;
import com.eps.entity.Product;
import com.eps.entity.Supplier;
import com.eps.enums.SupplierStatus;
import com.eps.service.ProductService;
import com.eps.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplierController {

    private final SupplierService supplierService;
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponseDto> createSupplier(@RequestBody SupplierDto dto) {
        Product product = null;
        if (dto.getProductId() != null) {
            product = productService.getProductById(dto.getProductId());
        }
        Supplier supplier = new Supplier();
        supplier.setProduct(product);
        supplier.setName(dto.getName());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setEmail(dto.getEmail());
        supplier.setGstNumber(dto.getGstNumber());
        supplier.setStatus(dto.getStatus() != null ? dto.getStatus() : SupplierStatus.ACTIVE);
        supplier.setRating(dto.getRating());
        supplier.setFeedback(dto.getFeedback());

        Supplier created = supplierService.createSupplier(supplier);
        SupplierDto responseDto = toDto(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.builder().success(true).message("Supplier created").data(responseDto).build());
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto> getAllSuppliers() {
        List<SupplierDto> suppliers = supplierService.getAllSuppliers().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Suppliers fetched").data(suppliers).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto> getSupplierById(@PathVariable Long id) {
        Supplier supplier = supplierService.getSupplierById(id);
        SupplierDto responseDto = toDto(supplier);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Supplier fetched").data(responseDto).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto> updateSupplier(@PathVariable Long id, @RequestBody SupplierDto dto) {
        Product product = null;
        if (dto.getProductId() != null) {
            product = productService.getProductById(dto.getProductId());
        }
        Supplier supplier = new Supplier();
        supplier.setProduct(product);
        supplier.setName(dto.getName());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setEmail(dto.getEmail());
        supplier.setGstNumber(dto.getGstNumber());
        supplier.setStatus(dto.getStatus() != null ? dto.getStatus() : SupplierStatus.ACTIVE);
        supplier.setRating(dto.getRating());
        supplier.setFeedback(dto.getFeedback());

        Supplier updated = supplierService.updateSupplier(id, supplier);
        SupplierDto responseDto = toDto(updated);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Supplier updated").data(responseDto).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Supplier deleted").build());
    }

    private SupplierDto toDto(Supplier supplier) {
        SupplierDto dto = new SupplierDto();
        dto.setSupplierId(supplier.getSupplierId());
        dto.setProductId(supplier.getProduct() != null ? supplier.getProduct().getProductId() : null);
        dto.setName(supplier.getName());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        dto.setEmail(supplier.getEmail());
        dto.setGstNumber(supplier.getGstNumber());
        dto.setStatus(supplier.getStatus());
        dto.setRating(supplier.getRating());
        dto.setFeedback(supplier.getFeedback());
        return dto;
    }
}
