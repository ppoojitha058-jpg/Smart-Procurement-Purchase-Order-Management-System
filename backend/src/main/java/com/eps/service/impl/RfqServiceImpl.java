package com.eps.service.impl;

import com.eps.entity.PurchaseRequest;
import com.eps.entity.Rfq;
import com.eps.repository.PurchaseRequestRepository;
import com.eps.repository.RfqRepository;
import com.eps.service.NotificationService;
import com.eps.service.RfqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RfqServiceImpl implements RfqService {

    private final RfqRepository rfqRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Rfq createRfq(Long purchaseRequestId, String title, String description) {
        PurchaseRequest pr = purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new RuntimeException("Purchase Request not found with ID: " + purchaseRequestId));

        pr.setStatus("RFQ_CREATED");
        purchaseRequestRepository.save(pr);

        Rfq rfq = new Rfq();
        rfq.setPurchaseRequest(pr);
        rfq.setTitle(title != null && !title.isBlank() ? title : "RFQ for Request #" + purchaseRequestId + " (" + pr.getProduct().getName() + ")");
        rfq.setDescription(description != null ? description : "Requesting competitive quotations for " + pr.getQuantity() + " units of " + pr.getProduct().getName());
        rfq.setStatus("OPEN");
        rfq.setCreatedDate(LocalDateTime.now());

        Rfq savedRfq = rfqRepository.save(rfq);

        notificationService.sendNotification(
                null,
                "SUPPLIER",
                "New RFQ Available",
                "A new RFQ #" + savedRfq.getRfqId() + " (" + savedRfq.getTitle() + ") is open for quotation bids."
        );

        return savedRfq;
    }

    @Override
    public List<Rfq> getAllRfqs() {
        return rfqRepository.findAll();
    }

    @Override
    public List<Rfq> getOpenRfqs() {
        return rfqRepository.findByStatus("OPEN");
    }

    @Override
    public Rfq getRfqById(Long rfqId) {
        return rfqRepository.findById(rfqId)
                .orElseThrow(() -> new RuntimeException("RFQ not found with ID: " + rfqId));
    }

    @Override
    public Rfq closeRfq(Long rfqId) {
        Rfq rfq = getRfqById(rfqId);
        rfq.setStatus("CLOSED");
        return rfqRepository.save(rfq);
    }
}
