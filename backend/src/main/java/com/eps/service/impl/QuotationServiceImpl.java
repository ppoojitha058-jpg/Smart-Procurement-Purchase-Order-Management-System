package com.eps.service.impl;

import com.eps.entity.PurchaseOrder;
import com.eps.entity.PurchaseRequest;
import com.eps.entity.Quotation;
import com.eps.entity.Rfq;
import com.eps.entity.Supplier;
import com.eps.repository.PurchaseOrderRepository;
import com.eps.repository.PurchaseRequestRepository;
import com.eps.repository.QuotationRepository;
import com.eps.repository.RfqRepository;
import com.eps.repository.SupplierRepository;
import com.eps.service.NotificationService;
import com.eps.service.QuotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationServiceImpl implements QuotationService {

    private final QuotationRepository quotationRepository;
    private final RfqRepository rfqRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Quotation submitQuotation(Long rfqId, Long supplierId, BigDecimal quotedPrice, Integer deliveryDays, String comments) {
        Rfq rfq = rfqRepository.findById(rfqId)
                .orElseThrow(() -> new RuntimeException("RFQ not found with ID: " + rfqId));

        if (!"OPEN".equalsIgnoreCase(rfq.getStatus())) {
            throw new RuntimeException("Cannot submit quotation. RFQ is not OPEN.");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found with ID: " + supplierId));

        Quotation quotation = new Quotation();
        quotation.setRfq(rfq);
        quotation.setSupplier(supplier);
        quotation.setQuotedPrice(quotedPrice);
        quotation.setDeliveryDays(deliveryDays != null ? deliveryDays : 7);
        quotation.setComments(comments);
        quotation.setStatus("PENDING");
        quotation.setCreatedDate(LocalDateTime.now());

        Quotation saved = quotationRepository.save(quotation);

        notificationService.sendNotification(
                null,
                "PROCUREMENT",
                "Quotation Received",
                "Supplier " + supplier.getName() + " submitted a quotation of $" + quotedPrice + " for RFQ #" + rfqId
        );

        return saved;
    }

    @Override
    public List<Quotation> getQuotationsForRfq(Long rfqId) {
        return quotationRepository.findByRfqRfqId(rfqId);
    }

    @Override
    public List<Quotation> getQuotationsBySupplier(Long supplierId) {
        return quotationRepository.findBySupplierSupplierId(supplierId);
    }

    @Override
    public Quotation getQuotationById(Long quotationId) {
        return quotationRepository.findById(quotationId)
                .orElseThrow(() -> new RuntimeException("Quotation not found with ID: " + quotationId));
    }

    @Override
    @Transactional
    public PurchaseOrder selectQuotation(Long quotationId) {
        Quotation selectedQuote = getQuotationById(quotationId);
        Rfq rfq = selectedQuote.getRfq();

        selectedQuote.setStatus("ACCEPTED");
        quotationRepository.save(selectedQuote);

        // Reject other quotations for this RFQ
        List<Quotation> allQuotes = quotationRepository.findByRfqRfqId(rfq.getRfqId());
        for (Quotation q : allQuotes) {
            if (!q.getQuotationId().equals(quotationId)) {
                q.setStatus("REJECTED");
                quotationRepository.save(q);
            }
        }

        rfq.setStatus("CLOSED");
        rfqRepository.save(rfq);

        PurchaseRequest pr = rfq.getPurchaseRequest();
        pr.setStatus("ORDERED");
        purchaseRequestRepository.save(pr);

        PurchaseOrder po = new PurchaseOrder();
        po.setPurchaseRequest(pr);
        po.setSupplier(selectedQuote.getSupplier());
        po.setTotalAmount(selectedQuote.getQuotedPrice());
        po.setStatus("CREATED");
        po.setCreatedDate(LocalDateTime.now());
        po.setUpdatedDate(LocalDateTime.now());

        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        // Notify Supplier
        notificationService.sendNotification(
                selectedQuote.getSupplier().getEmail(),
                "SUPPLIER",
                "Quotation Accepted - PO #" + savedPo.getOrderId() + " Created",
                "Congratulations! Your quotation for RFQ #" + rfq.getRfqId() + " was accepted. Purchase Order #" + savedPo.getOrderId() + " has been issued."
        );

        // Notify Employee who initiated the requisition
        if (pr.getUser() != null) {
            notificationService.sendNotification(
                    pr.getUser().getEmail(),
                    "USER",
                    "Purchase Order Issued",
                    "Purchase Order #" + savedPo.getOrderId() + " has been issued to " + selectedQuote.getSupplier().getName() + " for your requisition #" + pr.getRequestId()
            );
        }

        return savedPo;
    }
}
