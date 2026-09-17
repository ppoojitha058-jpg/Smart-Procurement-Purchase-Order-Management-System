package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.dto.ProductDto;
import com.eps.entity.Category;
import com.eps.entity.Department;
import com.eps.entity.Product;
import com.eps.entity.User;
import com.eps.enums.ProductStatus;
import com.eps.service.CategoryService;
import com.eps.service.DepartmentService;
import com.eps.service.ProductService;
import com.eps.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final UserService userService;
    private final DepartmentService departmentService;
    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto> createProduct(@RequestBody ProductDto dto) {
        Product product = buildProduct(dto);
        Product created = productService.createProduct(product);
        ProductDto responseDto = toDto(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.builder().success(true).message("Product created").data(responseDto).build());
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        List<ProductDto> dtos = products.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Products fetched").data(dtos).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        ProductDto responseDto = toDto(product);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Product fetched").data(responseDto).build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto> updateProduct(@PathVariable Long id, @RequestBody ProductDto dto) {
        Product product = buildProduct(dto);
        Product updated = productService.updateProduct(id, product);
        ProductDto responseDto = toDto(updated);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Product updated").data(responseDto).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Product deleted").build());
    }

    private Product buildProduct(ProductDto dto) {
        if (dto.getUserId() == null) {
            throw new RuntimeException("User ID is required");
        }
        if (dto.getDepartmentId() == null) {
            throw new RuntimeException("Department ID is required");
        }
        if (dto.getCategoryId() == null) {
            throw new RuntimeException("Category ID is required");
        }

        User user = userService.getUserById(dto.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
        Department department = departmentService.getDepartmentById(dto.getDepartmentId());
        Category category = categoryService.getCategoryById(dto.getCategoryId());

        Product product = new Product();
        product.setName(dto.getName());
        product.setSku(dto.getSku());
        product.setImageUrl(dto.getImageUrl());
        product.setPricePerProduct(dto.getPricePerProduct());
        product.setNumberOfQuantities(dto.getNumberOfQuantities());
        product.setDescription(dto.getDescription());
        product.setStatus(dto.getStatus() != null ? dto.getStatus() : ProductStatus.ACTIVE);
        product.setUser(user);
        product.setDepartment(department);
        product.setCategory(category);
        return product;
    }

    private ProductDto toDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setProductId(product.getProductId());
        dto.setName(product.getName());
        dto.setSku(product.getSku());
        dto.setImageUrl(product.getImageUrl());
        dto.setUserId(product.getUser() != null ? product.getUser().getUserId() : null);
        dto.setPricePerProduct(product.getPricePerProduct());
        dto.setNumberOfQuantities(product.getNumberOfQuantities());
        dto.setDepartmentId(product.getDepartment() != null ? product.getDepartment().getDepartmentId() : null);
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null);
        dto.setCategoryName((product.getCategory() != null) ? product.getCategory().getCategoryName() : "General");
        dto.setDescription(product.getDescription());
        dto.setStatus(product.getStatus());
        return dto;
    }
}
