package com.eps.service.impl;

import com.eps.entity.PurchaseOrder;
import com.eps.entity.PurchaseRequest;
import com.eps.repository.PurchaseOrderRepository;
import com.eps.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;

    @Override
    public PurchaseOrder createFromRequest(PurchaseRequest request) {
        PurchaseOrder po = new PurchaseOrder();
        po.setPurchaseRequest(request);
        po.setTotalAmount(request.getTotalPrice());
        return purchaseOrderRepository.save(po);
    }
}
