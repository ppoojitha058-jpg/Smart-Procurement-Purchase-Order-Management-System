package com.eps.service.impl;

import com.eps.entity.AuditLog;
import com.eps.entity.PurchaseOrder;
import com.eps.entity.PurchaseRequest;
import com.eps.repository.PurchaseRequestRepository;
import com.eps.service.AuditLogService;
import com.eps.service.EmailService;
import com.eps.service.NotificationService;
import com.eps.service.PurchaseOrderService;
import com.eps.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final EmailService emailService;
    private final PurchaseOrderService purchaseOrderService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public PurchaseRequest createPurchaseRequest(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getQuantity() != null && purchaseRequest.getProduct() != null && purchaseRequest.getProduct().getPricePerProduct() != null) {
            purchaseRequest.setTotalPrice(purchaseRequest.getProduct().getPricePerProduct().multiply(BigDecimal.valueOf(purchaseRequest.getQuantity())));
        }
        PurchaseRequest savedRequest = purchaseRequestRepository.save(purchaseRequest);

        // ── Real-time WebSocket broadcast (LIFO: newest request pushed to all subscribers) ──
        try {
            Map<String, Object> wsPayload = new HashMap<>();
            wsPayload.put("requestId", savedRequest.getRequestId());
            wsPayload.put("status", savedRequest.getStatus());
            wsPayload.put("userName", savedRequest.getUser() != null ? savedRequest.getUser().getFullName() : "User");
            wsPayload.put("productName", savedRequest.getProduct() != null ? savedRequest.getProduct().getName() : "Item");
            wsPayload.put("quantity", savedRequest.getQuantity());
            wsPayload.put("totalPrice", savedRequest.getTotalPrice());
            messagingTemplate.convertAndSend("/topic/purchase-requests", wsPayload);
        } catch (Exception ex) {
            log.warn("WebSocket broadcast failed: {}", ex.getMessage());
        }

        // In-app notification for Manager
        notificationService.sendNotification(
                null,
                "MANAGER",
                "New Purchase Request #" + savedRequest.getRequestId(),
                "Requisition for " + savedRequest.getQuantity() + "x " + (savedRequest.getProduct() != null ? savedRequest.getProduct().getName() : "Item") + " submitted by " + (savedRequest.getUser() != null ? savedRequest.getUser().getFullName() : "User") + " awaits your review."
        );

        // send confirmation email to requestor
        try {
            if (savedRequest.getUser() != null && savedRequest.getUser().getEmail() != null) {
                emailService.sendHtmlEmailFromTemplate(
                        savedRequest.getUser().getEmail(),
                        "Purchase Request Submitted",
                        "purchase-request-submitted",
                        Map.of(
                                "fullName", savedRequest.getUser().getFullName(),
                                "requestId", String.valueOf(savedRequest.getRequestId()),
                                "productName", savedRequest.getProduct() != null ? savedRequest.getProduct().getName() : "N/A",
                                "quantity", String.valueOf(savedRequest.getQuantity()),
                                "totalPrice", savedRequest.getTotalPrice() != null ? savedRequest.getTotalPrice().toString() : "N/A",
                                "departmentName", savedRequest.getDepartment() != null ? savedRequest.getDepartment().getDepartmentName() : "N/A",
                                "status", savedRequest.getStatus()
                        )
                );
            }
        } catch (Exception ex) {
            log.warn("Could not send confirmation email: {}", ex.getMessage());
        }

        return savedRequest;
    }

    @Override
    public List<PurchaseRequest> getPendingRequests() {
        return purchaseRequestRepository.findByStatus("PENDING");
    }

    @Override
    public List<PurchaseRequest> getAllPurchaseRequests() {
        return purchaseRequestRepository.findAll();
    }

    @Override
    public List<PurchaseRequest> getRequestsByUser(String email) {
        return purchaseRequestRepository.findByUserEmail(email);
    }

    @Override
    public List<PurchaseRequest> getRequestsByDepartment(Long departmentId) {
        return purchaseRequestRepository.findByDepartmentDepartmentId(departmentId);
    }

    @Override
    public List<PurchaseRequest> getRequestsByStatus(String status) {
        return purchaseRequestRepository.findByStatus(status);
    }

    @Override
    public List<PurchaseRequest> getRequestsByManager(String managerEmail) {
        return purchaseRequestRepository.findByApprovedByOrRejectedBy(managerEmail, managerEmail);
    }

    @Override
    public List<PurchaseRequest> getPendingRequestsByDepartment(Long departmentId) {
        return purchaseRequestRepository.findByDepartmentDepartmentIdAndStatus(departmentId, "PENDING");
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
        existing.setApprovedBy(managerEmail);
        existing.setApprovalDate(LocalDateTime.now());
        existing.setManagerComments(remarks != null ? remarks : "Approved by Manager");
        existing.setRejectedBy(null);
        existing.setRejectionDate(null);
        PurchaseRequest saved = purchaseRequestRepository.save(existing);

        // In-app notifications
        if (saved.getUser() != null) {
            notificationService.sendNotification(
                    saved.getUser().getEmail(),
                    "USER",
                    "Purchase Request #" + saved.getRequestId() + " Approved",
                    "Your requisition #" + saved.getRequestId() + " has been approved by your manager."
            );
        }
        notificationService.sendNotification(
                null,
                "PROCUREMENT",
                "Request #" + saved.getRequestId() + " Ready for RFQ",
                "Manager approved purchase request #" + saved.getRequestId() + ". Ready for procurement sourcing and RFQ creation."
        );

        // send approval email to user
        try {
            if (saved.getUser() != null && saved.getUser().getEmail() != null) {
                emailService.sendHtmlEmailFromTemplate(
                        saved.getUser().getEmail(),
                        "Purchase Request Approved",
                        "purchase-request-approved",
                        Map.of(
                                "fullName", saved.getUser().getFullName(),
                                "requestId", String.valueOf(saved.getRequestId()),
                                "productName", saved.getProduct() != null ? saved.getProduct().getName() : "N/A",
                                "quantity", String.valueOf(saved.getQuantity()),
                                "totalPrice", saved.getTotalPrice() != null ? saved.getTotalPrice().toString() : "N/A",
                                "remarks", remarks != null ? remarks : "No additional remarks",
                                "status", saved.getStatus()
                        )
                );
            }
        } catch (Exception ex) {
            log.warn("Could not send approval email: {}", ex.getMessage());
        }

        // audit log
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("APPROVE_REQUEST");
        auditLog.setPerformedBy(managerEmail);
        auditLog.setDetails("RequestId=" + saved.getRequestId() + ";Remarks=" + (remarks != null ? remarks : ""));
        auditLogService.save(auditLog);

        return saved;
    }

    @Override
    public PurchaseRequest rejectRequest(Long id, String managerEmail, String remarks) {
        PurchaseRequest existing = getPurchaseRequestById(id);
        existing.setStatus("REJECTED");
        existing.setRejectedBy(managerEmail);
        existing.setRejectionDate(LocalDateTime.now());
        existing.setManagerComments(remarks != null ? remarks : "Rejected by Manager");
        existing.setApprovedBy(null);
        existing.setApprovalDate(null);
        PurchaseRequest saved = purchaseRequestRepository.save(existing);

        if (saved.getUser() != null) {
            notificationService.sendNotification(
                    saved.getUser().getEmail(),
                    "USER",
                    "Purchase Request #" + saved.getRequestId() + " Rejected",
                    "Your requisition #" + saved.getRequestId() + " was rejected. Reason: " + (remarks != null ? remarks : "N/A")
            );
        }

        // send rejection email to user
        try {
            if (saved.getUser() != null && saved.getUser().getEmail() != null) {
                emailService.sendHtmlEmailFromTemplate(
                        saved.getUser().getEmail(),
                        "Purchase Request Rejected",
                        "purchase-request-rejected",
                        Map.of(
                                "fullName", saved.getUser().getFullName(),
                                "requestId", String.valueOf(saved.getRequestId()),
                                "productName", saved.getProduct() != null ? saved.getProduct().getName() : "N/A",
                                "quantity", String.valueOf(saved.getQuantity()),
                                "totalPrice", saved.getTotalPrice() != null ? saved.getTotalPrice().toString() : "N/A",
                                "remarks", remarks != null ? remarks : "No additional remarks",
                                "status", saved.getStatus()
                        )
                );
            }
        } catch (Exception ex) {
            log.warn("Could not send rejection email: {}", ex.getMessage());
        }

        // audit log
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("REJECT_REQUEST");
        auditLog.setPerformedBy(managerEmail);
        auditLog.setDetails("RequestId=" + saved.getRequestId() + ";Remarks=" + (remarks != null ? remarks : ""));
        auditLogService.save(auditLog);

        return saved;
    }

    @Override
    public void deletePurchaseRequest(Long id) {
        purchaseRequestRepository.deleteById(id);
    }
}
