package com.eps.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestDto {
    private Long requestId;
    private Long userId;
    private Long productId;
    private Long departmentId;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String status;
}
