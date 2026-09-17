package com.eps.service.impl;

import com.eps.entity.GoodsReceipt;
import com.eps.entity.Invoice;
import com.eps.entity.PurchaseOrder;
import com.eps.repository.GoodsReceiptRepository;
import com.eps.repository.InvoiceRepository;
import com.eps.repository.PurchaseOrderRepository;
import com.eps.service.InvoiceService;
import com.eps.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Invoice submitInvoice(Long orderId, String invoiceNumber, BigDecimal amount, String notes) {
        PurchaseOrder po = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found with ID: " + orderId));

        Invoice invoice = invoiceRepository.findByPurchaseOrderOrderId(orderId)
                .orElse(new Invoice());

        invoice.setPurchaseOrder(po);
        invoice.setSupplier(po.getSupplier());
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setAmount(amount != null ? amount : po.getTotalAmount());
        invoice.setStatus("SUBMITTED");
        invoice.setMatchingStatus("PENDING");
        invoice.setNotes(notes);
        if (invoice.getCreatedDate() == null) {
            invoice.setCreatedDate(LocalDateTime.now());
        }
        invoice.setUpdatedDate(LocalDateTime.now());

        Invoice saved = invoiceRepository.save(invoice);

        notificationService.sendNotification(
                null,
                "FINANCE",
                "New Invoice Submitted: " + invoiceNumber,
                "Invoice #" + invoiceNumber + " submitted for PO #" + orderId + " by " + (po.getSupplier() != null ? po.getSupplier().getName() : "Supplier") + " for $" + invoice.getAmount()
        );

        return saved;
    }

    @Override
    @Transactional
    public Map<String, Object> performThreeWayMatch(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        PurchaseOrder po = invoice.getPurchaseOrder();

        Map<String, Object> result = new HashMap<>();
        result.put("invoiceId", invoice.getInvoiceId());
        result.put("invoiceNumber", invoice.getInvoiceNumber());
        result.put("invoiceAmount", invoice.getAmount());

        if (po == null) {
            result.put("matched", false);
            result.put("reason", "No Purchase Order linked to this invoice");
            invoice.setMatchingStatus("MISMATCH");
            invoiceRepository.save(invoice);
            return result;
        }

        result.put("orderId", po.getOrderId());
        result.put("poAmount", po.getTotalAmount());
        Integer expectedQty = (po.getPurchaseRequest() != null) ? po.getPurchaseRequest().getQuantity() : 0;
        result.put("orderedQuantity", expectedQty);

        List<GoodsReceipt> grns = goodsReceiptRepository.findByPurchaseOrderOrderId(po.getOrderId());
        int totalReceived = grns.stream().mapToInt(g -> g.getQuantityReceived() != null ? g.getQuantityReceived() : 0).sum();
        result.put("receivedQuantity", totalReceived);

        boolean quantityMatched = totalReceived >= expectedQty && expectedQty > 0;
        boolean amountMatched = invoice.getAmount() != null && po.getTotalAmount() != null &&
                invoice.getAmount().compareTo(po.getTotalAmount()) == 0;

        result.put("quantityMatched", quantityMatched);
        result.put("amountMatched", amountMatched);

        if (quantityMatched && amountMatched) {
            invoice.setMatchingStatus("MATCHED");
            invoice.setStatus("MATCHED");
            result.put("matched", true);
            result.put("status", "MATCHED");
            result.put("message", "3-Way Match Verified: PO amount, GRN received quantity, and Invoice amount are consistent.");
        } else {
            invoice.setMatchingStatus("MISMATCH");
            result.put("matched", false);
            result.put("status", "MISMATCH");
            StringBuilder reason = new StringBuilder("Mismatch identified: ");
            if (!quantityMatched) reason.append("Received quantity (").append(totalReceived).append(") does not match ordered (").append(expectedQty).append("). ");
            if (!amountMatched) reason.append("Invoice amount ($").append(invoice.getAmount()).append(") does not match PO amount ($").append(po.getTotalAmount()).append(").");
            result.put("reason", reason.toString().trim());
        }

        invoiceRepository.save(invoice);
        return result;
    }

    @Override
    @Transactional
    public Invoice updateInvoiceStatus(Long invoiceId, String status, String notes) {
        Invoice invoice = getInvoiceById(invoiceId);
        invoice.setStatus(status.toUpperCase());
        if (notes != null) {
            invoice.setNotes(notes);
        }
        invoice.setUpdatedDate(LocalDateTime.now());
        Invoice saved = invoiceRepository.save(invoice);

        if ("APPROVED".equalsIgnoreCase(status) && invoice.getSupplier() != null) {
            notificationService.sendNotification(
                    invoice.getSupplier().getEmail(),
                    "SUPPLIER",
                    "Invoice #" + invoice.getInvoiceNumber() + " Approved",
                    "Your invoice #" + invoice.getInvoiceNumber() + " has been approved by Finance and is queued for payment."
            );
        }

        return saved;
    }

    @Override
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Override
    public List<Invoice> getInvoicesBySupplier(Long supplierId) {
        return invoiceRepository.findBySupplierSupplierId(supplierId);
    }

    @Override
    public Invoice getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));
    }
}
