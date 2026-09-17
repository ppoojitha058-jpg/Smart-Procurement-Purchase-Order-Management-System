package com.eps.service;

import java.io.Writer;

public interface CsvExportService {
    // User role-specific exports
    void writeUserRequestsCsv(String userEmail, Writer writer);
    void writeUserOrdersCsv(String userEmail, Writer writer);
    void writeUserPaymentsCsv(String userEmail, Writer writer);
    void writeUserTrackingCsv(String userEmail, Writer writer);

    // Manager role-specific exports
    void writeManagerRequestsCsv(String managerEmail, Long departmentId, Writer writer);
    void writeManagerApprovalHistoryCsv(String managerEmail, Writer writer);

    // Admin role-specific exports
    void writeAdminAllRequestsCsv(Writer writer);
    void writeAdminOrdersCsv(Writer writer);
    void writeAdminSuppliersCsv(Writer writer);
    void writeAdminPaymentsCsv(Writer writer);
    void writeAdminTransactionsCsv(Writer writer);
    void writeAdminTrackingCsv(Writer writer);

    // Supplier role-specific exports
    void writeSupplierOrdersCsv(Long supplierId, String supplierEmail, Writer writer);
    void writeSupplierPaymentsCsv(Long supplierId, String supplierEmail, Writer writer);
    void writeSupplierDeliveryCsv(Long supplierId, String supplierEmail, Writer writer);
    void writeSupplierTrackingCsv(Long supplierId, String supplierEmail, Writer writer);

    // Legacy convenience methods
    void writeDepartmentRequestsCsv(Long departmentId, Writer writer);
    void writeAllRequestsCsv(Writer writer);
    void writeAllOrdersCsv(Writer writer);
}
