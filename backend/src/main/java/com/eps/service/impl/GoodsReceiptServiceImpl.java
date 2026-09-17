package com.eps.service.impl;

import com.eps.entity.GoodsReceipt;
import com.eps.entity.PurchaseOrder;
import com.eps.repository.GoodsReceiptRepository;
import com.eps.repository.PurchaseOrderRepository;
import com.eps.service.GoodsReceiptService;
import com.eps.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public GoodsReceipt createGoodsReceipt(Long orderId, String receivedBy, Integer quantityReceived, String conditionStatus, String notes) {
        PurchaseOrder po = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found with ID: " + orderId));

        GoodsReceipt grn = new GoodsReceipt();
        grn.setPurchaseOrder(po);
        grn.setReceivedBy(receivedBy);
        grn.setQuantityReceived(quantityReceived);
        grn.setConditionStatus(conditionStatus != null ? conditionStatus : "GOOD");
        grn.setNotes(notes);
        grn.setReceivedDate(LocalDateTime.now());

        GoodsReceipt saved = goodsReceiptRepository.save(grn);

        po.setStatus("DELIVERED");
        po.setUpdatedDate(LocalDateTime.now());
        purchaseOrderRepository.save(po);

        notificationService.sendNotification(
                null,
                "FINANCE",
                "Goods Received for PO #" + orderId,
                "Receiving completed for PO #" + orderId + ". Quantity: " + quantityReceived + ", Condition: " + grn.getConditionStatus() + ". Ready for 3-way match."
        );

        if (po.getPurchaseRequest() != null && po.getPurchaseRequest().getUser() != null) {
            notificationService.sendNotification(
                    po.getPurchaseRequest().getUser().getEmail(),
                    "USER",
                    "Items Delivered & Inspected",
                    "Your requested items under PO #" + orderId + " have been received and verified."
            );
        }

        return saved;
    }

    @Override
    public List<GoodsReceipt> getAllGoodsReceipts() {
        return goodsReceiptRepository.findAll();
    }

    @Override
    public List<GoodsReceipt> getGoodsReceiptsByOrderId(Long orderId) {
        return goodsReceiptRepository.findByPurchaseOrderOrderId(orderId);
    }

    @Override
    public GoodsReceipt getGoodsReceiptById(Long grnId) {
        return goodsReceiptRepository.findById(grnId)
                .orElseThrow(() -> new RuntimeException("Goods Receipt not found with ID: " + grnId));
    }
}
