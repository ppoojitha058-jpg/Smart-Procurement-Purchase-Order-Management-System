package com.eps.repository;

import com.eps.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByInvoiceInvoiceId(Long invoiceId);
    List<Payment> findByStatus(String status);
    long countByStatus(String status);
    List<Payment> findByInvoicePurchaseOrderPurchaseRequestUserEmail(String userEmail);
    List<Payment> findByInvoiceSupplierSupplierId(Long supplierId);
    List<Payment> findByInvoiceSupplierEmail(String supplierEmail);
}
