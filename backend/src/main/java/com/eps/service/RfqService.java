package com.eps.service;

import com.eps.entity.Rfq;

import java.util.List;

public interface RfqService {
    Rfq createRfq(Long purchaseRequestId, String title, String description);
    List<Rfq> getAllRfqs();
    List<Rfq> getOpenRfqs();
    Rfq getRfqById(Long rfqId);
    Rfq closeRfq(Long rfqId);
}
