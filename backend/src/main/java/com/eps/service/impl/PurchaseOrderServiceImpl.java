package com.eps.service.impl;

import com.eps.entity.PurchaseOrder;
import com.eps.entity.PurchaseRequest;
import com.eps.repository.PurchaseOrderRepository;
import com.eps.service.NotificationService;
import com.eps.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final NotificationService notificationService;

    @Override
    public PurchaseOrder createFromRequest(PurchaseRequest request) {
        PurchaseOrder po = new PurchaseOrder();
        po.setPurchaseRequest(request);
        po.setTotalAmount(request.getTotalPrice());
        po.setStatus("CREATED");
        po.setCreatedDate(LocalDateTime.now());
        po.setUpdatedDate(LocalDateTime.now());
        return purchaseOrderRepository.save(po);
    }

    @Override
    public PurchaseOrder getOrderById(Long orderId) {
        return purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found with ID: " + orderId));
    }

    @Override
    public List<PurchaseOrder> getAllOrders() {
        return purchaseOrderRepository.findAll();
    }

    @Override
    public List<PurchaseOrder> getOrdersBySupplier(Long supplierId) {
        return purchaseOrderRepository.findBySupplierSupplierId(supplierId);
    }

    @Override
    public List<PurchaseOrder> getOrdersByUser(String userEmail) {
        return purchaseOrderRepository.findByPurchaseRequestUserEmail(userEmail);
    }

    @Override
    public List<PurchaseOrder> getOrdersByDepartment(Long departmentId) {
        return purchaseOrderRepository.findByPurchaseRequestDepartmentDepartmentId(departmentId);
    }

    @Override
    public List<PurchaseOrder> getOrdersByStatus(String status) {
        return purchaseOrderRepository.findByStatus(status);
    }

    @Override
    @Transactional
    public PurchaseOrder acceptOrder(Long orderId) {
        PurchaseOrder po = getOrderById(orderId);
        po.setStatus("ACCEPTED");
        po.setUpdatedDate(LocalDateTime.now());
        PurchaseOrder saved = purchaseOrderRepository.save(po);

        notificationService.sendNotification(
                null,
                "PROCUREMENT",
                "PO #" + orderId + " Accepted",
                "Supplier accepted Purchase Order #" + orderId + "."
        );

        return saved;
    }

    @Override
    @Transactional
    public PurchaseOrder updateShipment(Long orderId, String trackingNumber, String shipmentDetails) {
        PurchaseOrder po = getOrderById(orderId);
        po.setStatus("SHIPPED");
        po.setTrackingNumber(trackingNumber);
        po.setShipmentDetails(shipmentDetails);
        po.setUpdatedDate(LocalDateTime.now());
        PurchaseOrder saved = purchaseOrderRepository.save(po);

        notificationService.sendNotification(
                null,
                "RECEIVING",
                "Shipment Dispatched for PO #" + orderId,
                "PO #" + orderId + " has been shipped. Tracking: " + trackingNumber + ". Ready for receiving inspection."
        );

        if (po.getPurchaseRequest() != null && po.getPurchaseRequest().getUser() != null) {
            notificationService.sendNotification(
                    po.getPurchaseRequest().getUser().getEmail(),
                    "USER",
                    "Your Order Has Shipped!",
                    "PO #" + orderId + " is in transit with tracking number " + trackingNumber + "."
            );
        }

        return saved;
    }

    @Override
    @Transactional
    public PurchaseOrder updateStatus(Long orderId, String status) {
        PurchaseOrder po = getOrderById(orderId);
        po.setStatus(status);
        po.setUpdatedDate(LocalDateTime.now());
        return purchaseOrderRepository.save(po);
    }
}
