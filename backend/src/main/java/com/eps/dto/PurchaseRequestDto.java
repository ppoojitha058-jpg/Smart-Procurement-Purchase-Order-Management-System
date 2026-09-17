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
    private String productName;
    private Long departmentId;
    private String departmentName;
    private Integer quantity;
    private BigDecimal totalPrice;
    private BigDecimal unitPrice;
    private String sku;
    private String categoryName;
    private String imageUrl;
    private String status;
    private String createdDate;
    private String trackingNumber;
    private String carrier;
    private String poNumber;
    private String managerApproval;
    private String estimatedDelivery;
    private String approvedBy;
    private String rejectedBy;
    private String approvalDate;
    private String rejectionDate;
    private String managerComments;
    private String requesterName;
    private String businessJustification;
    private String priority;
}
