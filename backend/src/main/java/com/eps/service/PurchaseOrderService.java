package com.eps.service;

import com.eps.entity.PurchaseOrder;
import com.eps.entity.PurchaseRequest;

import java.util.List;

public interface PurchaseOrderService {
    PurchaseOrder createFromRequest(PurchaseRequest request);
    PurchaseOrder getOrderById(Long orderId);
    List<PurchaseOrder> getAllOrders();
    List<PurchaseOrder> getOrdersBySupplier(Long supplierId);
    List<PurchaseOrder> getOrdersByUser(String userEmail);
    List<PurchaseOrder> getOrdersByDepartment(Long departmentId);
    List<PurchaseOrder> getOrdersByStatus(String status);
    PurchaseOrder acceptOrder(Long orderId);
    PurchaseOrder updateShipment(Long orderId, String trackingNumber, String shipmentDetails);
    PurchaseOrder updateStatus(Long orderId, String status);
}
