package com.eps.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "goods_receipt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grn_id")
    private Long grnId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "received_by", nullable = false, length = 100)
    private String receivedBy;

    @Column(name = "quantity_received", nullable = false)
    private Integer quantityReceived;

    @Column(name = "condition_status", length = 50)
    private String conditionStatus = "GOOD"; // GOOD, DAMAGED, PARTIAL

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    @PrePersist
    protected void onCreate() {
        if (receivedDate == null) {
            receivedDate = LocalDateTime.now();
        }
        if (conditionStatus == null) {
            conditionStatus = "GOOD";
        }
    }
}
