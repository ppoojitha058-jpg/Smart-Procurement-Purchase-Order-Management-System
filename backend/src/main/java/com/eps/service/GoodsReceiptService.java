package com.eps.service;

import com.eps.entity.GoodsReceipt;

import java.util.List;

public interface GoodsReceiptService {
    GoodsReceipt createGoodsReceipt(Long orderId, String receivedBy, Integer quantityReceived, String conditionStatus, String notes);
    List<GoodsReceipt> getAllGoodsReceipts();
    List<GoodsReceipt> getGoodsReceiptsByOrderId(Long orderId);
    GoodsReceipt getGoodsReceiptById(Long grnId);
}
