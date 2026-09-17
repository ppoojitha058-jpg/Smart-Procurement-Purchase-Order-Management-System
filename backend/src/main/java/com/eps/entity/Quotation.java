package com.eps.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quotation_id")
    private Long quotationId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "quoted_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal quotedPrice;

    @Column(name = "delivery_days")
    private Integer deliveryDays;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "status", length = 50)
    private String status = "PENDING"; // PENDING, ACCEPTED, REJECTED

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}
