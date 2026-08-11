package com.eps.service;

import com.eps.entity.PurchaseOrder;
import com.eps.entity.PurchaseRequest;

public interface PurchaseOrderService {
    PurchaseOrder createFromRequest(PurchaseRequest request);
}
