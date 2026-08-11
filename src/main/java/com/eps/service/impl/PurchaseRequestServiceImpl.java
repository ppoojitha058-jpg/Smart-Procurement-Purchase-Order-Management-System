package com.eps.service.impl;

import com.eps.entity.PurchaseOrder;
import com.eps.entity.PurchaseRequest;
import com.eps.repository.PurchaseRequestRepository;
import com.eps.service.AuditLogService;
import com.eps.service.EmailService;
import com.eps.service.PurchaseOrderService;
import com.eps.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.eps.entity.AuditLog;

@Service
@RequiredArgsConstructor
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final EmailService emailService;
    private final PurchaseOrderService purchaseOrderService;
    private final AuditLogService auditLogService;

    @Override
    public PurchaseRequest createPurchaseRequest(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getQuantity() != null && purchaseRequest.getProduct() != null && purchaseRequest.getProduct().getPricePerProduct() != null) {
            purchaseRequest.setTotalPrice(purchaseRequest.getProduct().getPricePerProduct().multiply(BigDecimal.valueOf(purchaseRequest.getQuantity())));
        }
        PurchaseRequest savedRequest = purchaseRequestRepository.save(purchaseRequest);

        // send confirmation email to requestor
        try {
            emailService.sendHtmlEmailFromTemplate(
                    savedRequest.getUser().getEmail(),
                    "Purchase Request Submitted",
                    "purchase-request-submitted",
                    Map.of(
                            "fullName", savedRequest.getUser().getFullName(),
                            "requestId", String.valueOf(savedRequest.getRequestId()),
                            "productName", savedRequest.getProduct().getName(),
                            "quantity", String.valueOf(savedRequest.getQuantity()),
                            "totalPrice", savedRequest.getTotalPrice() != null ? savedRequest.getTotalPrice().toString() : "N/A",
                            "departmentName", savedRequest.getDepartment() != null ? savedRequest.getDepartment().getDepartmentName() : "N/A",
                            "status", savedRequest.getStatus()
                    )
            );
        } catch (Exception ex) {
            // log but do not fail
        }

        // notify manager if configured
        if (savedRequest.getDepartment() != null && savedRequest.getDepartment().getManagerOfDepartment() != null) {
            String managerContact = savedRequest.getDepartment().getManagerOfDepartment();
            if (managerContact.contains("@")) {
                try {
                    emailService.sendHtmlEmailFromTemplate(
                            managerContact,
                            "New Purchase Request Pending Approval",
                            "new-purchase-request-manager",
                            Map.of(
                                    "managerName", managerContact,
                                    "requestId", String.valueOf(savedRequest.getRequestId()),
                                    "userName", savedRequest.getUser().getFullName(),
                                    "productName", savedRequest.getProduct().getName(),
                                    "quantity", String.valueOf(savedRequest.getQuantity()),
                                    "totalPrice", savedRequest.getTotalPrice() != null ? savedRequest.getTotalPrice().toString() : "N/A",
                                    "departmentName", savedRequest.getDepartment().getDepartmentName(),
                                    "status", savedRequest.getStatus()
                            )
                    );
                } catch (Exception ex) {
                    // log but do not fail
                }
            }
        }

        return savedRequest;
    }

    @Override
    public List<PurchaseRequest> getPendingRequests() {
        return purchaseRequestRepository.findAll().stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseRequest> getAllPurchaseRequests() {
        return purchaseRequestRepository.findAll();
    }

    @Override
    public PurchaseRequest getPurchaseRequestById(Long id) {
        return purchaseRequestRepository.findById(id).orElseThrow(() -> new RuntimeException("Purchase request not found"));
    }

    @Override
    public PurchaseRequest updatePurchaseRequest(Long id, PurchaseRequest purchaseRequest) {
        PurchaseRequest existing = getPurchaseRequestById(id);
        existing.setQuantity(purchaseRequest.getQuantity());
        existing.setStatus(purchaseRequest.getStatus());
        existing.setDepartment(purchaseRequest.getDepartment());
        existing.setProduct(purchaseRequest.getProduct());
        if (existing.getProduct() != null && existing.getProduct().getPricePerProduct() != null && existing.getQuantity() != null) {
            existing.setTotalPrice(existing.getProduct().getPricePerProduct().multiply(BigDecimal.valueOf(existing.getQuantity())));
        }
        return purchaseRequestRepository.save(existing);
    }

    @Override
    public PurchaseRequest approveRequest(Long id, String managerEmail, String remarks) {
        PurchaseRequest existing = getPurchaseRequestById(id);
        existing.setStatus("APPROVED");
        PurchaseRequest saved = purchaseRequestRepository.save(existing);

        // create purchase order
        PurchaseOrder po = purchaseOrderService.createFromRequest(saved);

        // send approval email to user
        try {
            emailService.sendHtmlEmailFromTemplate(
                    saved.getUser().getEmail(),
                    "Purchase Request Approved",
                    "purchase-request-approved",
                    Map.of(
                            "fullName", saved.getUser().getFullName(),
                            "requestId", String.valueOf(saved.getRequestId()),
                            "productName", saved.getProduct().getName(),
                            "quantity", String.valueOf(saved.getQuantity()),
                            "totalPrice", saved.getTotalPrice() != null ? saved.getTotalPrice().toString() : "N/A",
                            "remarks", remarks != null ? remarks : "No additional remarks",
                            "status", saved.getStatus()
                    )
            );
        } catch (Exception ex) {
            // log but do not fail
        }

        // send purchase order generated email to user
        try {
            emailService.sendHtmlEmailFromTemplate(
                    saved.getUser().getEmail(),
                    "Purchase Order Generated",
                    "purchase-order-generated",
                    Map.of(
                            "fullName", saved.getUser().getFullName(),
                            "orderId", po.getOrderId() != null ? String.valueOf(po.getOrderId()) : "N/A",
                            "requestId", String.valueOf(saved.getRequestId()),
                            "productName", saved.getProduct().getName(),
                            "quantity", String.valueOf(saved.getQuantity()),
                            "totalAmount", po.getTotalAmount() != null ? po.getTotalAmount().toString() : "N/A"
                    )
            );
        } catch (Exception ex) {
            // log but do not fail
        }

        // audit log
        AuditLog log = new AuditLog();
        log.setAction("APPROVE_REQUEST");
        log.setPerformedBy(managerEmail);
        log.setDetails("RequestId=" + saved.getRequestId() + ";Remarks=" + (remarks != null ? remarks : ""));
        auditLogService.save(log);

        return saved;
    }

    @Override
    public PurchaseRequest rejectRequest(Long id, String managerEmail, String remarks) {
        PurchaseRequest existing = getPurchaseRequestById(id);
        existing.setStatus("REJECTED");
        PurchaseRequest saved = purchaseRequestRepository.save(existing);

        // send rejection email to user
        try {
            emailService.sendHtmlEmailFromTemplate(
                    saved.getUser().getEmail(),
                    "Purchase Request Rejected",
                    "purchase-request-rejected",
                    Map.of(
                            "fullName", saved.getUser().getFullName(),
                            "requestId", String.valueOf(saved.getRequestId()),
                            "productName", saved.getProduct().getName(),
                            "quantity", String.valueOf(saved.getQuantity()),
                            "totalPrice", saved.getTotalPrice() != null ? saved.getTotalPrice().toString() : "N/A",
                            "remarks", remarks != null ? remarks : "No additional remarks",
                            "status", saved.getStatus()
                    )
            );
        } catch (Exception ex) {
            // log but do not fail
        }

        // audit log
        AuditLog log = new AuditLog();
        log.setAction("REJECT_REQUEST");
        log.setPerformedBy(managerEmail);
        log.setDetails("RequestId=" + saved.getRequestId() + ";Remarks=" + (remarks != null ? remarks : ""));
        auditLogService.save(log);

        return saved;
    }

    @Override
    public void deletePurchaseRequest(Long id) {
        purchaseRequestRepository.deleteById(id);
    }
}
