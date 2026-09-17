package com.eps.repository;

import com.eps.entity.PurchaseOrder;
import com.eps.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findBySupplierSupplierId(Long supplierId);
    List<PurchaseOrder> findBySupplier(Supplier supplier);
    List<PurchaseOrder> findByStatus(String status);
    long countByStatus(String status);
    Optional<PurchaseOrder> findByPurchaseRequestRequestId(Long requestId);
    List<PurchaseOrder> findByPurchaseRequestUserEmail(String email);
    List<PurchaseOrder> findByPurchaseRequestDepartmentDepartmentId(Long departmentId);
}
