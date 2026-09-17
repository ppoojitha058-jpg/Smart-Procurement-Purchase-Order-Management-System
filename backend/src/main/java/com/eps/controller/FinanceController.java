package com.eps.controller;

import com.eps.entity.Invoice;
import com.eps.entity.Payment;
import com.eps.service.InvoiceService;
import com.eps.service.PaymentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FinanceController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    @Data
    public static class UpdateStatusRequest {
        private String status;
        private String notes;
    }

    @Data
    public static class ProcessPaymentRequest {
        private String paymentMethod;
        private String transactionReference;
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/invoices/{id}/match")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> performMatch(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.performThreeWayMatch(id));
    }

    @PutMapping("/invoices/{id}/status")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<Invoice> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(invoiceService.updateInvoiceStatus(id, request.getStatus(), request.getNotes()));
    }

    @PostMapping("/invoices/{id}/payment")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<Payment> processPayment(@PathVariable Long id, @RequestBody(required = false) ProcessPaymentRequest request) {
        String method = request != null ? request.getPaymentMethod() : "BANK_TRANSFER";
        String txnRef = request != null ? request.getTransactionReference() : null;
        return ResponseEntity.ok(paymentService.processPayment(id, method, txnRef));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/payments/{id}")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }
}
