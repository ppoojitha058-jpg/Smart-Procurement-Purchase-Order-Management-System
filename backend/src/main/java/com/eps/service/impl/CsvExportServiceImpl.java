package com.eps.service.impl;

import com.eps.entity.*;
import com.eps.repository.*;
import com.eps.service.CsvExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvExportServiceImpl implements CsvExportService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final AuditLogRepository auditLogRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String escape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private Long authenticatedSupplierId(String supplierEmail) {
        if (supplierEmail == null || supplierEmail.isBlank()) return null;
        return supplierRepository.findByEmailIgnoreCase(supplierEmail)
                .map(Supplier::getSupplierId)
                .orElse(null);
    }

    private List<PurchaseOrder> supplierOrders(Long supplierId, String supplierEmail) {
        Long scopedId = supplierId != null ? supplierId : authenticatedSupplierId(supplierEmail);
        return scopedId == null ? List.of() : purchaseOrderRepository.findBySupplierSupplierId(scopedId);
    }

    private List<Payment> supplierPayments(Long supplierId, String supplierEmail) {
        Long scopedId = supplierId != null ? supplierId : authenticatedSupplierId(supplierEmail);
        return scopedId == null ? List.of() : paymentRepository.findByInvoiceSupplierSupplierId(scopedId);
    }

    // ==========================================
    // USER ROLE CSV EXPORTS
    // ==========================================

    @Override
    public void writeUserRequestsCsv(String userEmail, Writer writer) {
        try {
            writer.write("Request ID,Request Date,Product Name,Category,Quantity,Amount,Department,Description,Request Status,Manager Approval Status\n");
            List<PurchaseRequest> list = purchaseRequestRepository.findByUserEmail(userEmail);
            for (PurchaseRequest pr : list) {
                String created = pr.getCreatedDate() != null ? pr.getCreatedDate().format(DATE_FORMATTER) : "N/A";
                String prod = pr.getProduct() != null ? escape(pr.getProduct().getName()) : "N/A";
                String cat = (pr.getProduct() != null && pr.getProduct().getCategory() != null) ? escape(pr.getProduct().getCategory().getCategoryName()) : "N/A";
                String dept = pr.getDepartment() != null ? escape(pr.getDepartment().getDepartmentName()) : "N/A";
                String mgr = pr.getDepartment() != null ? escape(pr.getDepartment().getManagerOfDepartment()) : "N/A";
                String desc = (pr.getProduct() != null && pr.getProduct().getDescription() != null) ? escape(pr.getProduct().getDescription()) : "";

                writer.write(String.format("%d,%s,%s,%s,%d,%.2f,%s,%s,%s,%s\n",
                        pr.getRequestId(),
                        created,
                        prod,
                        cat,
                        pr.getQuantity() != null ? pr.getQuantity() : 0,
                        pr.getTotalPrice() != null ? pr.getTotalPrice().doubleValue() : 0.0,
                        dept,
                        desc,
                        escape(pr.getStatus()),
                        mgr
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing user requests CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    @Override
    public void writeUserOrdersCsv(String userEmail, Writer writer) {
        try {
            writer.write("Order ID,Request ID,Product Name,Quantity,Amount,Supplier,Order Date,Order Status,Tracking Number,Delivery Status\n");
            List<PurchaseOrder> list = purchaseOrderRepository.findByPurchaseRequestUserEmail(userEmail);
            for (PurchaseOrder po : list) {
                String reqId = po.getPurchaseRequest() != null ? String.valueOf(po.getPurchaseRequest().getRequestId()) : "N/A";
                String prod = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getProduct() != null) ? escape(po.getPurchaseRequest().getProduct().getName()) : "N/A";
                int qty = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getQuantity() != null) ? po.getPurchaseRequest().getQuantity() : 0;
                String supplierName = po.getSupplier() != null ? escape(po.getSupplier().getName()) : "Unassigned";
                String created = po.getCreatedDate() != null ? po.getCreatedDate().format(DATE_FORMATTER) : "N/A";

                writer.write(String.format("%d,%s,%s,%d,%.2f,%s,%s,%s,%s,%s\n",
                        po.getOrderId(),
                        reqId,
                        prod,
                        qty,
                        po.getTotalAmount() != null ? po.getTotalAmount().doubleValue() : 0.0,
                        supplierName,
                        created,
                        escape(po.getStatus()),
                        escape(po.getTrackingNumber()),
                        escape(po.getStatus().equalsIgnoreCase("DELIVERED") || po.getStatus().equalsIgnoreCase("COMPLETED") ? "DELIVERED" : po.getStatus())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing user orders CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    @Override
    public void writeUserPaymentsCsv(String userEmail, Writer writer) {
        try {
            writer.write("Payment ID,Request ID,Order ID,Amount,Payment Date,Payment Method,Payment Status,Transaction Reference\n");
            List<Payment> list = paymentRepository.findByInvoicePurchaseOrderPurchaseRequestUserEmail(userEmail);
            for (Payment p : list) {
                String reqId = (p.getInvoice() != null && p.getInvoice().getPurchaseOrder() != null && p.getInvoice().getPurchaseOrder().getPurchaseRequest() != null) ?
                        String.valueOf(p.getInvoice().getPurchaseOrder().getPurchaseRequest().getRequestId()) : "N/A";
                String orderId = (p.getInvoice() != null && p.getInvoice().getPurchaseOrder() != null) ?
                        String.valueOf(p.getInvoice().getPurchaseOrder().getOrderId()) : "N/A";
                String pDate = p.getPaymentDate() != null ? p.getPaymentDate().format(DATE_FORMATTER) : "N/A";

                writer.write(String.format("%d,%s,%s,%.2f,%s,%s,%s,%s\n",
                        p.getPaymentId(),
                        reqId,
                        orderId,
                        p.getAmount() != null ? p.getAmount().doubleValue() : 0.0,
                        pDate,
                        escape(p.getPaymentMethod()),
                        escape(p.getStatus()),
                        escape(p.getTransactionReference())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing user payments CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    @Override
    public void writeUserTrackingCsv(String userEmail, Writer writer) {
        try {
            writer.write("Request ID,Order ID,Current Status,Stage,Status Date,Supplier,Tracking Number,Delivery Details\n");
            List<PurchaseOrder> list = purchaseOrderRepository.findByPurchaseRequestUserEmail(userEmail);
            for (PurchaseOrder po : list) {
                String reqId = po.getPurchaseRequest() != null ? String.valueOf(po.getPurchaseRequest().getRequestId()) : "N/A";
                String supplierName = po.getSupplier() != null ? escape(po.getSupplier().getName()) : "Pending Supplier Selection";
                String updated = po.getUpdatedDate() != null ? po.getUpdatedDate().format(DATE_FORMATTER) : (po.getCreatedDate() != null ? po.getCreatedDate().format(DATE_FORMATTER) : "N/A");

                writer.write(String.format("%s,%d,%s,%s,%s,%s,%s,%s\n",
                        reqId,
                        po.getOrderId(),
                        escape(po.getStatus()),
                        escape(mapStage(po.getStatus())),
                        updated,
                        supplierName,
                        escape(po.getTrackingNumber()),
                        escape(po.getShipmentDetails())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing user tracking CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    // ==========================================
    // MANAGER ROLE CSV EXPORTS
    // ==========================================

    @Override
    public void writeManagerRequestsCsv(String managerEmail, Long departmentId, Writer writer) {
        try {
            writer.write("Request ID,Requester Name,Requester Email,Department,Product Name,Category,Quantity,Amount,Request Date,Status,Approval Status\n");
            List<PurchaseRequest> list = (departmentId != null) ?
                    purchaseRequestRepository.findByDepartmentDepartmentId(departmentId) :
                    purchaseRequestRepository.findAll();

            for (PurchaseRequest pr : list) {
                String userName = pr.getUser() != null ? escape(pr.getUser().getFullName()) : "N/A";
                String userEmail = pr.getUser() != null ? escape(pr.getUser().getEmail()) : "N/A";
                String dept = pr.getDepartment() != null ? escape(pr.getDepartment().getDepartmentName()) : "N/A";
                String prod = pr.getProduct() != null ? escape(pr.getProduct().getName()) : "N/A";
                String cat = (pr.getProduct() != null && pr.getProduct().getCategory() != null) ? escape(pr.getProduct().getCategory().getCategoryName()) : "N/A";
                String created = pr.getCreatedDate() != null ? pr.getCreatedDate().format(DATE_FORMATTER) : "N/A";

                writer.write(String.format("%d,%s,%s,%s,%s,%s,%d,%.2f,%s,%s,%s\n",
                        pr.getRequestId(),
                        userName,
                        userEmail,
                        dept,
                        prod,
                        cat,
                        pr.getQuantity() != null ? pr.getQuantity() : 0,
                        pr.getTotalPrice() != null ? pr.getTotalPrice().doubleValue() : 0.0,
                        created,
                        escape(pr.getStatus()),
                        escape(pr.getStatus().equalsIgnoreCase("APPROVED") ? "APPROVED" : (pr.getStatus().equalsIgnoreCase("REJECTED") ? "REJECTED" : "PENDING_APPROVAL"))
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing manager requests CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    @Override
    public void writeManagerApprovalHistoryCsv(String managerEmail, Writer writer) {
        try {
            writer.write("Audit ID,Request ID,Action,Decision,Performed By,Date,Details\n");
            List<AuditLog> allLogs = auditLogRepository.findByActionContaining("REQUEST");
            for (AuditLog logItem : allLogs) {
                if (managerEmail != null && !managerEmail.isEmpty() && !managerEmail.equals(logItem.getPerformedBy())) {
                    continue;
                }
                String created = logItem.getCreatedDate() != null ? logItem.getCreatedDate().format(DATE_FORMATTER) : "N/A";
                String requestId = extractRequestId(logItem.getDetails());
                writer.write(String.format("%d,%s,%s,%s,%s,%s,%s\n",
                        logItem.getId(),
                        requestId,
                        escape(logItem.getAction()),
                        escape(logItem.getAction().contains("APPROVE") ? "APPROVED" : "REJECTED"),
                        escape(logItem.getPerformedBy()),
                        created,
                        escape(logItem.getDetails())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing manager approval history CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    // ==========================================
    // ADMIN ROLE CSV EXPORTS
    // ==========================================

    @Override
    public void writeAdminAllRequestsCsv(Writer writer) {
        try {
            writer.write("Request ID,User/Requester,Requester Email,Department,Product,Category,Quantity,Amount,Request Date,Manager,Current Status\n");
            List<PurchaseRequest> list = purchaseRequestRepository.findAll();
            for (PurchaseRequest pr : list) {
                String userName = pr.getUser() != null ? escape(pr.getUser().getFullName()) : "N/A";
                String userEmail = pr.getUser() != null ? escape(pr.getUser().getEmail()) : "N/A";
                String dept = pr.getDepartment() != null ? escape(pr.getDepartment().getDepartmentName()) : "N/A";
                String prod = pr.getProduct() != null ? escape(pr.getProduct().getName()) : "N/A";
                String cat = (pr.getProduct() != null && pr.getProduct().getCategory() != null) ? escape(pr.getProduct().getCategory().getCategoryName()) : "N/A";
                String created = pr.getCreatedDate() != null ? pr.getCreatedDate().format(DATE_FORMATTER) : "N/A";
                String mgr = pr.getDepartment() != null ? escape(pr.getDepartment().getManagerOfDepartment()) : "N/A";

                writer.write(String.format("%d,%s,%s,%s,%s,%s,%d,%.2f,%s,%s,%s\n",
                        pr.getRequestId(),
                        userName,
                        userEmail,
                        dept,
                        prod,
                        cat,
                        pr.getQuantity() != null ? pr.getQuantity() : 0,
                        pr.getTotalPrice() != null ? pr.getTotalPrice().doubleValue() : 0.0,
                        created,
                        mgr,
                        escape(pr.getStatus())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing admin all requests CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    @Override
    public void writeAdminOrdersCsv(Writer writer) {
        try {
            writer.write("Order ID,Request ID,User,Department,Product,Quantity,Amount,Supplier,Order Date,Order Status,Delivery Status\n");
            List<PurchaseOrder> list = purchaseOrderRepository.findAll();
            for (PurchaseOrder po : list) {
                String reqId = po.getPurchaseRequest() != null ? String.valueOf(po.getPurchaseRequest().getRequestId()) : "N/A";
                String user = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getUser() != null) ? escape(po.getPurchaseRequest().getUser().getFullName()) : "N/A";
                String dept = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getDepartment() != null) ? escape(po.getPurchaseRequest().getDepartment().getDepartmentName()) : "N/A";
                String prod = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getProduct() != null) ? escape(po.getPurchaseRequest().getProduct().getName()) : "N/A";
                int qty = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getQuantity() != null) ? po.getPurchaseRequest().getQuantity() : 0;
                String supplierName = po.getSupplier() != null ? escape(po.getSupplier().getName()) : "N/A";
                String created = po.getCreatedDate() != null ? po.getCreatedDate().format(DATE_FORMATTER) : "N/A";

                writer.write(String.format("%d,%s,%s,%s,%s,%d,%.2f,%s,%s,%s,%s\n",
                        po.getOrderId(),
                        reqId,
                        user,
                        dept,
                        prod,
                        qty,
                        po.getTotalAmount() != null ? po.getTotalAmount().doubleValue() : 0.0,
                        supplierName,
                        created,
                        escape(po.getStatus()),
                        escape(po.getStatus().equalsIgnoreCase("DELIVERED") || po.getStatus().equalsIgnoreCase("COMPLETED") ? "DELIVERED" : po.getStatus())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing admin orders CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    @Override
    public void writeAdminSuppliersCsv(Writer writer) {
        try {
            writer.write("Supplier ID,Supplier Name,Email,Phone,Address,GST Number,Status,Rating,Feedback\n");
            List<Supplier> list = supplierRepository.findAll();
            for (Supplier s : list) {
                writer.write(String.format("%d,%s,%s,%s,%s,%s,%s,%.1f,%s\n",
                        s.getSupplierId(),
                        escape(s.getName()),
                        escape(s.getEmail()),
                        escape(s.getPhone()),
                        escape(s.getAddress()),
                        escape(s.getGstNumber()),
                        s.getStatus() != null ? s.getStatus().name() : "ACTIVE",
                        s.getRating() != null ? s.getRating().doubleValue() : 5.0,
                        escape(s.getFeedback())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing admin suppliers CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    @Override
    public void writeAdminPaymentsCsv(Writer writer) {
        try {
            writer.write("Payment ID,Request ID,Order ID,User,Supplier,Amount,Payment Date,Payment Method,Transaction Reference,Payment Status\n");
            List<Payment> list = paymentRepository.findAll();
            for (Payment p : list) {
                String reqId = (p.getInvoice() != null && p.getInvoice().getPurchaseOrder() != null && p.getInvoice().getPurchaseOrder().getPurchaseRequest() != null) ?
                        String.valueOf(p.getInvoice().getPurchaseOrder().getPurchaseRequest().getRequestId()) : "N/A";
                String orderId = (p.getInvoice() != null && p.getInvoice().getPurchaseOrder() != null) ?
                        String.valueOf(p.getInvoice().getPurchaseOrder().getOrderId()) : "N/A";
                String user = (p.getInvoice() != null && p.getInvoice().getPurchaseOrder() != null && p.getInvoice().getPurchaseOrder().getPurchaseRequest() != null && p.getInvoice().getPurchaseOrder().getPurchaseRequest().getUser() != null) ?
                        escape(p.getInvoice().getPurchaseOrder().getPurchaseRequest().getUser().getFullName()) : "N/A";
                String supplierName = (p.getInvoice() != null && p.getInvoice().getSupplier() != null) ?
                        escape(p.getInvoice().getSupplier().getName()) : "N/A";
                String pDate = p.getPaymentDate() != null ? p.getPaymentDate().format(DATE_FORMATTER) : "N/A";

                writer.write(String.format("%d,%s,%s,%s,%s,%.2f,%s,%s,%s,%s\n",
                        p.getPaymentId(),
                        reqId,
                        orderId,
                        user,
                        supplierName,
                        p.getAmount() != null ? p.getAmount().doubleValue() : 0.0,
                        pDate,
                        escape(p.getPaymentMethod()),
                        escape(p.getTransactionReference()),
                        escape(p.getStatus())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing admin payments CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    @Override
    public void writeAdminTransactionsCsv(Writer writer) {
        try {
            writer.write("Transaction ID,Request ID,Order ID,User,Supplier,Amount,Transaction Date,Transaction Type,Status\n");
            List<Payment> list = paymentRepository.findAll();
            for (Payment p : list) {
                String reqId = (p.getInvoice() != null && p.getInvoice().getPurchaseOrder() != null && p.getInvoice().getPurchaseOrder().getPurchaseRequest() != null) ?
                        String.valueOf(p.getInvoice().getPurchaseOrder().getPurchaseRequest().getRequestId()) : "N/A";
                String orderId = (p.getInvoice() != null && p.getInvoice().getPurchaseOrder() != null) ?
                        String.valueOf(p.getInvoice().getPurchaseOrder().getOrderId()) : "N/A";
                String user = (p.getInvoice() != null && p.getInvoice().getPurchaseOrder() != null && p.getInvoice().getPurchaseOrder().getPurchaseRequest() != null && p.getInvoice().getPurchaseOrder().getPurchaseRequest().getUser() != null) ?
                        escape(p.getInvoice().getPurchaseOrder().getPurchaseRequest().getUser().getFullName()) : "N/A";
                String supplierName = (p.getInvoice() != null && p.getInvoice().getSupplier() != null) ?
                        escape(p.getInvoice().getSupplier().getName()) : "N/A";
                String pDate = p.getPaymentDate() != null ? p.getPaymentDate().format(DATE_FORMATTER) : "N/A";

                writer.write(String.format("%s,%s,%s,%s,%s,%.2f,%s,%s,%s\n",
                        escape(p.getTransactionReference()),
                        reqId,
                        orderId,
                        user,
                        supplierName,
                        p.getAmount() != null ? p.getAmount().doubleValue() : 0.0,
                        pDate,
                        escape(p.getPaymentMethod()),
                        escape(p.getStatus())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing admin transactions CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    @Override
    public void writeAdminTrackingCsv(Writer writer) {
        try {
            writer.write("Request ID,Order ID,User,Supplier,Current Stage,Current Status,Status Date,Tracking Number,Shipment Details\n");
            List<PurchaseOrder> list = purchaseOrderRepository.findAll();
            for (PurchaseOrder po : list) {
                String reqId = po.getPurchaseRequest() != null ? String.valueOf(po.getPurchaseRequest().getRequestId()) : "N/A";
                String user = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getUser() != null) ? escape(po.getPurchaseRequest().getUser().getFullName()) : "N/A";
                String supplierName = po.getSupplier() != null ? escape(po.getSupplier().getName()) : "N/A";
                String updated = po.getUpdatedDate() != null ? po.getUpdatedDate().format(DATE_FORMATTER) : (po.getCreatedDate() != null ? po.getCreatedDate().format(DATE_FORMATTER) : "N/A");

                writer.write(String.format("%s,%d,%s,%s,%s,%s,%s,%s,%s\n",
                        reqId,
                        po.getOrderId(),
                        user,
                        supplierName,
                        escape(mapStage(po.getStatus())),
                        escape(po.getStatus()),
                        updated,
                        escape(po.getTrackingNumber()),
                        escape(po.getShipmentDetails())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing admin tracking CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    // ==========================================
    // SUPPLIER ROLE CSV EXPORTS
    // ==========================================

    @Override
    public void writeSupplierOrdersCsv(Long supplierId, String supplierEmail, Writer writer) {
        try {
            writer.write("Order ID,Request ID,Product,Category,Quantity,Amount,Customer/Department,Order Date,Order Status\n");
                List<PurchaseOrder> list = supplierOrders(supplierId, supplierEmail);

            for (PurchaseOrder po : list) {
                String reqId = po.getPurchaseRequest() != null ? String.valueOf(po.getPurchaseRequest().getRequestId()) : "N/A";
                String prod = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getProduct() != null) ? escape(po.getPurchaseRequest().getProduct().getName()) : "N/A";
                String cat = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getProduct() != null && po.getPurchaseRequest().getProduct().getCategory() != null) ?
                        escape(po.getPurchaseRequest().getProduct().getCategory().getCategoryName()) : "N/A";
                int qty = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getQuantity() != null) ? po.getPurchaseRequest().getQuantity() : 0;
                String dept = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getDepartment() != null) ? escape(po.getPurchaseRequest().getDepartment().getDepartmentName()) : "N/A";
                String created = po.getCreatedDate() != null ? po.getCreatedDate().format(DATE_FORMATTER) : "N/A";

                writer.write(String.format("%d,%s,%s,%s,%d,%.2f,%s,%s,%s\n",
                        po.getOrderId(),
                        reqId,
                        prod,
                        cat,
                        qty,
                        po.getTotalAmount() != null ? po.getTotalAmount().doubleValue() : 0.0,
                        dept,
                        created,
                        escape(po.getStatus())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing supplier orders CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    @Override
    public void writeSupplierPaymentsCsv(Long supplierId, String supplierEmail, Writer writer) {
        try {
            writer.write("Payment ID,Order ID,Request ID,Amount,Payment Date,Payment Method,Transaction Reference,Payment Status\n");
                List<Payment> list = supplierPayments(supplierId, supplierEmail);

            for (Payment p : list) {
                String reqId = (p.getInvoice() != null && p.getInvoice().getPurchaseOrder() != null && p.getInvoice().getPurchaseOrder().getPurchaseRequest() != null) ?
                        String.valueOf(p.getInvoice().getPurchaseOrder().getPurchaseRequest().getRequestId()) : "N/A";
                String orderId = (p.getInvoice() != null && p.getInvoice().getPurchaseOrder() != null) ?
                        String.valueOf(p.getInvoice().getPurchaseOrder().getOrderId()) : "N/A";
                String pDate = p.getPaymentDate() != null ? p.getPaymentDate().format(DATE_FORMATTER) : "N/A";

                writer.write(String.format("%d,%s,%s,%.2f,%s,%s,%s,%s\n",
                        p.getPaymentId(),
                        orderId,
                        reqId,
                        p.getAmount() != null ? p.getAmount().doubleValue() : 0.0,
                        pDate,
                        escape(p.getPaymentMethod()),
                        escape(p.getTransactionReference()),
                        escape(p.getStatus())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing supplier payments CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    @Override
    public void writeSupplierDeliveryCsv(Long supplierId, String supplierEmail, Writer writer) {
        try {
            writer.write("Order ID,Request ID,Product,Quantity,Order Date,Delivery Status,Tracking Number,Shipment Details\n");
                List<PurchaseOrder> list = supplierOrders(supplierId, supplierEmail);

            for (PurchaseOrder po : list) {
                String reqId = po.getPurchaseRequest() != null ? String.valueOf(po.getPurchaseRequest().getRequestId()) : "N/A";
                String prod = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getProduct() != null) ? escape(po.getPurchaseRequest().getProduct().getName()) : "N/A";
                int qty = (po.getPurchaseRequest() != null && po.getPurchaseRequest().getQuantity() != null) ? po.getPurchaseRequest().getQuantity() : 0;
                String created = po.getCreatedDate() != null ? po.getCreatedDate().format(DATE_FORMATTER) : "N/A";

                writer.write(String.format("%d,%s,%s,%d,%s,%s,%s,%s\n",
                        po.getOrderId(),
                        reqId,
                        prod,
                        qty,
                        created,
                        escape(po.getStatus()),
                        escape(po.getTrackingNumber()),
                        escape(po.getShipmentDetails())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing supplier delivery CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    @Override
    public void writeSupplierTrackingCsv(Long supplierId, String supplierEmail, Writer writer) {
        try {
            writer.write("Order ID,Request ID,Current Status,Stage,Status Date,Tracking Number,Shipment Details\n");
                List<PurchaseOrder> list = supplierOrders(supplierId, supplierEmail);

            for (PurchaseOrder po : list) {
                String reqId = po.getPurchaseRequest() != null ? String.valueOf(po.getPurchaseRequest().getRequestId()) : "N/A";
                String updated = po.getUpdatedDate() != null ? po.getUpdatedDate().format(DATE_FORMATTER) : (po.getCreatedDate() != null ? po.getCreatedDate().format(DATE_FORMATTER) : "N/A");

                writer.write(String.format("%d,%s,%s,%s,%s,%s,%s\n",
                        po.getOrderId(),
                        reqId,
                        escape(po.getStatus()),
                        escape(mapStage(po.getStatus())),
                        updated,
                        escape(po.getTrackingNumber()),
                        escape(po.getShipmentDetails())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing supplier tracking CSV", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    // ==========================================
    // LEGACY METHODS
    // ==========================================

    @Override
    public void writeDepartmentRequestsCsv(Long departmentId, Writer writer) {
        writeManagerRequestsCsv(null, departmentId, writer);
    }

    @Override
    public void writeAllRequestsCsv(Writer writer) {
        writeAdminAllRequestsCsv(writer);
    }

    @Override
    public void writeAllOrdersCsv(Writer writer) {
        writeAdminOrdersCsv(writer);
    }

    private String mapStage(String status) {
        if (status == null) return "Unknown";
        return switch (status.toUpperCase()) {
            case "PENDING" -> "Request Submitted";
            case "APPROVED" -> "Manager Approved";
            case "RFQ_CREATED" -> "Procurement Sourcing";
            case "ORDERED", "CREATED" -> "Purchase Order Issued";
            case "ACCEPTED" -> "Supplier Processing";
            case "SHIPPED" -> "In Transit";
            case "DELIVERED" -> "Receiving Inspection Complete";
            case "COMPLETED", "PAID" -> "Settled & Completed";
            case "REJECTED" -> "Requisition Rejected";
            default -> status;
        };
    }

    private String extractRequestId(String details) {
        if (details == null) return "N/A";
        try {
            int start = details.indexOf("RequestId=") + 11;
            int end = details.indexOf(";", start);
            if (end == -1) end = details.length();
            return details.substring(start, end);
        } catch (Exception e) {
            return "N/A";
        }
    }
}
