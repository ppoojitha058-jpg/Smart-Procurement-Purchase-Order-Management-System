package com.eps.service;

import com.eps.entity.Invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface InvoiceService {
    Invoice submitInvoice(Long orderId, String invoiceNumber, BigDecimal amount, String notes);
    Map<String, Object> performThreeWayMatch(Long invoiceId);
    Invoice updateInvoiceStatus(Long invoiceId, String status, String notes);
    List<Invoice> getAllInvoices();
    List<Invoice> getInvoicesBySupplier(Long supplierId);
    Invoice getInvoiceById(Long invoiceId);
}
