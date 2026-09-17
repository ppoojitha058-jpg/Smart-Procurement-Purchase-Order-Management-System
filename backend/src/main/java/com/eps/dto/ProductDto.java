package com.eps.dto;

import com.eps.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long productId;
    private String name;
    private Long userId;
    private BigDecimal pricePerProduct;
    private Integer numberOfQuantities;
    private Long departmentId;
    private Long categoryId;
    private String categoryName;
    private String sku;
    private String imageUrl;
    private String description;
    private ProductStatus status;
}
