package com.eps.service;

import com.eps.entity.PurchaseRequest;

import java.util.List;

public interface PurchaseRequestService {
    PurchaseRequest createPurchaseRequest(PurchaseRequest purchaseRequest);
    List<PurchaseRequest> getAllPurchaseRequests();
    PurchaseRequest getPurchaseRequestById(Long id);
    PurchaseRequest updatePurchaseRequest(Long id, PurchaseRequest purchaseRequest);
    void deletePurchaseRequest(Long id);

    // Manager actions
    List<PurchaseRequest> getPendingRequests();
    PurchaseRequest approveRequest(Long id, String managerEmail, String remarks);
    PurchaseRequest rejectRequest(Long id, String managerEmail, String remarks);
}
