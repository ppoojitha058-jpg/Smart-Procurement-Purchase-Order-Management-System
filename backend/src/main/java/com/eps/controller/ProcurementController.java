package com.eps.controller;

import com.eps.entity.PurchaseOrder;
import com.eps.entity.PurchaseRequest;
import com.eps.entity.Quotation;
import com.eps.entity.Rfq;
import com.eps.service.PurchaseOrderService;
import com.eps.service.PurchaseRequestService;
import com.eps.service.QuotationService;
import com.eps.service.RfqService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProcurementController {

    private final PurchaseRequestService purchaseRequestService;
    private final RfqService rfqService;
    private final QuotationService quotationService;
    private final PurchaseOrderService purchaseOrderService;

    @Data
    public static class CreateRfqRequest {
        private Long purchaseRequestId;
        private String title;
        private String description;
    }

    @GetMapping("/approved-requests")
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    public ResponseEntity<List<PurchaseRequest>> getApprovedRequests() {
        return ResponseEntity.ok(purchaseRequestService.getRequestsByStatus("APPROVED"));
    }

    @PostMapping("/rfq")
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    public ResponseEntity<Rfq> createRfq(@RequestBody CreateRfqRequest request) {
        Rfq created = rfqService.createRfq(request.getPurchaseRequestId(), request.getTitle(), request.getDescription());
        return ResponseEntity.ok(created);
    }

    @GetMapping("/rfq")
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN', 'SUPPLIER')")
    public ResponseEntity<List<Rfq>> getAllRfqs() {
        return ResponseEntity.ok(rfqService.getAllRfqs());
    }

    @GetMapping("/rfq/{id}")
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN', 'SUPPLIER')")
    public ResponseEntity<Rfq> getRfqById(@PathVariable Long id) {
        return ResponseEntity.ok(rfqService.getRfqById(id));
    }

    @GetMapping("/rfq/{id}/quotes")
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    public ResponseEntity<List<Quotation>> getQuotesForRfq(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.getQuotationsForRfq(id));
    }

    @PostMapping("/rfq/{id}/select-quote/{quotationId}")
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    public ResponseEntity<PurchaseOrder> selectQuote(@PathVariable Long id, @PathVariable Long quotationId) {
        PurchaseOrder po = quotationService.selectQuotation(quotationId);
        return ResponseEntity.ok(po);
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    public ResponseEntity<List<PurchaseOrder>> getAllOrders() {
        return ResponseEntity.ok(purchaseOrderService.getAllOrders());
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    public ResponseEntity<PurchaseOrder> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getOrderById(id));
    }
}
