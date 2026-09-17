package com.eps.service.impl;

import com.eps.entity.Invoice;
import com.eps.entity.Payment;
import com.eps.entity.PurchaseOrder;
import com.eps.repository.InvoiceRepository;
import com.eps.repository.PaymentRepository;
import com.eps.repository.PurchaseOrderRepository;
import com.eps.service.NotificationService;
import com.eps.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Payment processPayment(Long invoiceId, String paymentMethod, String transactionReference) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            throw new RuntimeException("Invoice #" + invoice.getInvoiceNumber() + " has already been paid.");
        }

        String txnRef = (transactionReference != null && !transactionReference.isBlank())
                ? transactionReference
                : "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(invoice.getAmount());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod : "BANK_TRANSFER");
        payment.setTransactionReference(txnRef);
        payment.setStatus("COMPLETED");

        Payment savedPayment = paymentRepository.save(payment);

        invoice.setStatus("PAID");
        invoice.setUpdatedDate(LocalDateTime.now());
        invoiceRepository.save(invoice);

        PurchaseOrder po = invoice.getPurchaseOrder();
        if (po != null) {
            po.setStatus("COMPLETED");
            po.setUpdatedDate(LocalDateTime.now());
            purchaseOrderRepository.save(po);
        }

        if (invoice.getSupplier() != null) {
            notificationService.sendNotification(
                    invoice.getSupplier().getEmail(),
                    "SUPPLIER",
                    "Payment Processed: $" + payment.getAmount(),
                    "Payment for Invoice #" + invoice.getInvoiceNumber() + " has been processed successfully. Txn Ref: " + txnRef
            );
        }

        return savedPayment;
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));
    }

    @Override
    public Payment getPaymentByInvoiceId(Long invoiceId) {
        return paymentRepository.findByInvoiceInvoiceId(invoiceId).orElse(null);
    }
}
