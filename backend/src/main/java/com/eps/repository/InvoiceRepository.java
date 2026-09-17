package com.eps.repository;

import com.eps.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findByPurchaseOrderOrderId(Long orderId);
    List<Invoice> findBySupplierSupplierId(Long supplierId);
    List<Invoice> findByStatus(String status);
}
