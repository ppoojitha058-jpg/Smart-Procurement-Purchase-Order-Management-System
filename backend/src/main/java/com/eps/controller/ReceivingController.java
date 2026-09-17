package com.eps.controller;

import com.eps.entity.GoodsReceipt;
import com.eps.entity.PurchaseOrder;
import com.eps.service.GoodsReceiptService;
import com.eps.service.PurchaseOrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/receiving")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReceivingController {

    private final GoodsReceiptService goodsReceiptService;
    private final PurchaseOrderService purchaseOrderService;

    @Data
    public static class GrnCreateRequest {
        private Long orderId;
        private String receivedBy;
        private Integer quantityReceived;
        private String conditionStatus;
        private String notes;
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('RECEIVING', 'ADMIN', 'PROCUREMENT')")
    public ResponseEntity<List<PurchaseOrder>> getReceivingOrders() {
        return ResponseEntity.ok(purchaseOrderService.getAllOrders());
    }

    @PostMapping("/grn")
    @PreAuthorize("hasAnyRole('RECEIVING', 'ADMIN')")
    public ResponseEntity<GoodsReceipt> createGrn(@RequestBody GrnCreateRequest request) {
        GoodsReceipt grn = goodsReceiptService.createGoodsReceipt(
                request.getOrderId(),
                request.getReceivedBy() != null ? request.getReceivedBy() : "Receiving Officer",
                request.getQuantityReceived(),
                request.getConditionStatus(),
                request.getNotes()
        );
        return ResponseEntity.ok(grn);
    }

    @GetMapping("/grn")
    @PreAuthorize("hasAnyRole('RECEIVING', 'ADMIN', 'FINANCE')")
    public ResponseEntity<List<GoodsReceipt>> getAllGrns() {
        return ResponseEntity.ok(goodsReceiptService.getAllGoodsReceipts());
    }

    @GetMapping("/grn/order/{orderId}")
    @PreAuthorize("hasAnyRole('RECEIVING', 'ADMIN', 'FINANCE')")
    public ResponseEntity<List<GoodsReceipt>> getGrnByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(goodsReceiptService.getGoodsReceiptsByOrderId(orderId));
    }
}
