package com.eps.dto;

import com.eps.enums.SupplierStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto {
    private Long supplierId;
    private Long productId;
    private String name;
    private String phone;
    private String address;
    private String email;
    private String gstNumber;
    private SupplierStatus status;
    private BigDecimal rating;
    private String feedback;
}
