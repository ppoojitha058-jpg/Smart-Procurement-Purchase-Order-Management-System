package com.eps.service;

import com.eps.entity.PurchaseOrder;
import com.eps.entity.Quotation;

import java.math.BigDecimal;
import java.util.List;

public interface QuotationService {
    Quotation submitQuotation(Long rfqId, Long supplierId, BigDecimal quotedPrice, Integer deliveryDays, String comments);
    List<Quotation> getQuotationsForRfq(Long rfqId);
    List<Quotation> getQuotationsBySupplier(Long supplierId);
    Quotation getQuotationById(Long quotationId);
    PurchaseOrder selectQuotation(Long quotationId);
}
