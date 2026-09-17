package com.eps.service;

import com.eps.entity.Payment;

import java.util.List;

public interface PaymentService {
    Payment processPayment(Long invoiceId, String paymentMethod, String transactionReference);
    List<Payment> getAllPayments();
    Payment getPaymentById(Long paymentId);
    Payment getPaymentByInvoiceId(Long invoiceId);
}
