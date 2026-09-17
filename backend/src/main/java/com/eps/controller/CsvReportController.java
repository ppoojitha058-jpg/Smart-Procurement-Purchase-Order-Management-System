package com.eps.controller;

import com.eps.service.CsvExportService;
import com.eps.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports/csv")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CsvReportController {

    private final CsvExportService csvExportService;
    private final UserService userService;

    private String getDateSuffix() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private void prepareCsvResponse(HttpServletResponse response, String filename) {
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    }

    private String resolveUserEmail(Authentication authentication, String paramEmail) {
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank() && !authentication.getName().equals("anonymousUser")) {
            return authentication.getName();
        }
        return (paramEmail != null && !paramEmail.isBlank()) ? paramEmail : "employee@eps.com";
    }

    private Long resolveSupplierId(Authentication authentication, Long requestedSupplierId) {
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return admin ? requestedSupplierId : null;
    }

    private Long resolveManagerDepartment(Authentication authentication, Long requestedDepartmentId) {
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (admin) return requestedDepartmentId;
        if (authentication == null) return null;
        return userService.getUserByEmail(authentication.getName())
                .map(user -> user.getDepartment() != null ? user.getDepartment().getDepartmentId() : null)
                .orElse(null);
    }

    // ==========================================
    // USER ROLE EXPORTS
    // ==========================================

    @GetMapping("/user/requests")
    public void downloadUserRequestsCsv(Authentication authentication,
                                       @RequestParam(required = false) String email,
                                       HttpServletResponse response) throws IOException {
        String targetEmail = resolveUserEmail(authentication, email);
        prepareCsvResponse(response, "user_requests_" + getDateSuffix() + ".csv");
        csvExportService.writeUserRequestsCsv(targetEmail, response.getWriter());
    }

    @GetMapping("/user/orders")
    public void downloadUserOrdersCsv(Authentication authentication,
                                     @RequestParam(required = false) String email,
                                     HttpServletResponse response) throws IOException {
        String targetEmail = resolveUserEmail(authentication, email);
        prepareCsvResponse(response, "user_orders_" + getDateSuffix() + ".csv");
        csvExportService.writeUserOrdersCsv(targetEmail, response.getWriter());
    }

    @GetMapping("/user/payments")
    public void downloadUserPaymentsCsv(Authentication authentication,
                                       @RequestParam(required = false) String email,
                                       HttpServletResponse response) throws IOException {
        String targetEmail = resolveUserEmail(authentication, email);
        prepareCsvResponse(response, "user_payments_" + getDateSuffix() + ".csv");
        csvExportService.writeUserPaymentsCsv(targetEmail, response.getWriter());
    }

    @GetMapping("/user/tracking")
    public void downloadUserTrackingCsv(Authentication authentication,
                                       @RequestParam(required = false) String email,
                                       HttpServletResponse response) throws IOException {
        String targetEmail = resolveUserEmail(authentication, email);
        prepareCsvResponse(response, "user_tracking_" + getDateSuffix() + ".csv");
        csvExportService.writeUserTrackingCsv(targetEmail, response.getWriter());
    }

    // Legacy user endpoint
    @GetMapping("/my-requests")
    public void downloadMyRequestsCsv(Authentication authentication,
                                      @RequestParam(required = false) String email,
                                      HttpServletResponse response) throws IOException {
        downloadUserRequestsCsv(authentication, email, response);
    }

    // ==========================================
    // MANAGER ROLE EXPORTS
    // ==========================================

    @GetMapping("/manager/requests")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public void downloadManagerRequestsCsv(Authentication authentication,
                                          @RequestParam(required = false) Long departmentId,
                                          HttpServletResponse response) throws IOException {
        String managerEmail = authentication != null ? authentication.getName() : null;
        prepareCsvResponse(response, "manager_requests_" + getDateSuffix() + ".csv");
        csvExportService.writeManagerRequestsCsv(managerEmail, resolveManagerDepartment(authentication, departmentId), response.getWriter());
    }

    @GetMapping("/manager/approvals")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public void downloadManagerApprovalsCsv(Authentication authentication,
                                           HttpServletResponse response) throws IOException {
        String managerEmail = authentication != null ? authentication.getName() : null;
        prepareCsvResponse(response, "manager_approvals_" + getDateSuffix() + ".csv");
        csvExportService.writeManagerApprovalHistoryCsv(managerEmail, response.getWriter());
    }

    // Legacy manager endpoint
    @GetMapping("/department-requests")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public void downloadDepartmentRequestsCsv(Authentication authentication, @RequestParam(required = false) Long departmentId,
                                             HttpServletResponse response) throws IOException {
        downloadManagerRequestsCsv(authentication, departmentId, response);
    }

    // ==========================================
    // ADMIN ROLE EXPORTS
    // ==========================================

    @GetMapping("/admin/requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT')")
    public void downloadAdminRequestsCsv(HttpServletResponse response) throws IOException {
        prepareCsvResponse(response, "admin_all_requests_" + getDateSuffix() + ".csv");
        csvExportService.writeAdminAllRequestsCsv(response.getWriter());
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT', 'FINANCE')")
    public void downloadAdminOrdersCsv(HttpServletResponse response) throws IOException {
        prepareCsvResponse(response, "admin_all_orders_" + getDateSuffix() + ".csv");
        csvExportService.writeAdminOrdersCsv(response.getWriter());
    }

    @GetMapping("/admin/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT')")
    public void downloadAdminSuppliersCsv(HttpServletResponse response) throws IOException {
        prepareCsvResponse(response, "admin_suppliers_" + getDateSuffix() + ".csv");
        csvExportService.writeAdminSuppliersCsv(response.getWriter());
    }

    @GetMapping("/admin/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public void downloadAdminPaymentsCsv(HttpServletResponse response) throws IOException {
        prepareCsvResponse(response, "admin_payments_" + getDateSuffix() + ".csv");
        csvExportService.writeAdminPaymentsCsv(response.getWriter());
    }

    @GetMapping("/admin/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public void downloadAdminTransactionsCsv(HttpServletResponse response) throws IOException {
        prepareCsvResponse(response, "admin_transactions_" + getDateSuffix() + ".csv");
        csvExportService.writeAdminTransactionsCsv(response.getWriter());
    }

    @GetMapping("/admin/tracking")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT', 'RECEIVING')")
    public void downloadAdminTrackingCsv(HttpServletResponse response) throws IOException {
        prepareCsvResponse(response, "admin_tracking_" + getDateSuffix() + ".csv");
        csvExportService.writeAdminTrackingCsv(response.getWriter());
    }

    // Legacy admin endpoints
    @GetMapping("/all-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT')")
    public void downloadAllRequestsCsv(HttpServletResponse response) throws IOException {
        downloadAdminRequestsCsv(response);
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT', 'FINANCE')")
    public void downloadOrdersCsv(HttpServletResponse response) throws IOException {
        downloadAdminOrdersCsv(response);
    }

    // ==========================================
    // SUPPLIER ROLE EXPORTS
    // ==========================================

    @GetMapping("/supplier/orders")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public void downloadSupplierOrdersCsv(Authentication authentication,
                                         @RequestParam(required = false) Long supplierId,
                                         HttpServletResponse response) throws IOException {
        String email = authentication != null ? authentication.getName() : null;
        prepareCsvResponse(response, "supplier_orders_" + getDateSuffix() + ".csv");
        csvExportService.writeSupplierOrdersCsv(resolveSupplierId(authentication, supplierId), email, response.getWriter());
    }

    @GetMapping("/supplier/payments")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN', 'FINANCE')")
    public void downloadSupplierPaymentsCsv(Authentication authentication,
                                           @RequestParam(required = false) Long supplierId,
                                           HttpServletResponse response) throws IOException {
        String email = authentication != null ? authentication.getName() : null;
        prepareCsvResponse(response, "supplier_payments_" + getDateSuffix() + ".csv");
        csvExportService.writeSupplierPaymentsCsv(resolveSupplierId(authentication, supplierId), email, response.getWriter());
    }

    @GetMapping("/supplier/delivery")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN', 'RECEIVING')")
    public void downloadSupplierDeliveryCsv(Authentication authentication,
                                           @RequestParam(required = false) Long supplierId,
                                           HttpServletResponse response) throws IOException {
        String email = authentication != null ? authentication.getName() : null;
        prepareCsvResponse(response, "supplier_delivery_" + getDateSuffix() + ".csv");
        csvExportService.writeSupplierDeliveryCsv(resolveSupplierId(authentication, supplierId), email, response.getWriter());
    }

    @GetMapping("/supplier/tracking")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public void downloadSupplierTrackingCsv(Authentication authentication,
                                           @RequestParam(required = false) Long supplierId,
                                           HttpServletResponse response) throws IOException {
        String email = authentication != null ? authentication.getName() : null;
        prepareCsvResponse(response, "supplier_tracking_" + getDateSuffix() + ".csv");
        csvExportService.writeSupplierTrackingCsv(resolveSupplierId(authentication, supplierId), email, response.getWriter());
    }
}
