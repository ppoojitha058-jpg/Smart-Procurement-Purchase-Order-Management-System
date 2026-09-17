package com.eps.repository;

import com.eps.entity.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    List<PurchaseRequest> findByUserEmail(String email);
    List<PurchaseRequest> findByDepartmentDepartmentId(Long departmentId);
    List<PurchaseRequest> findByStatus(String status);
    List<PurchaseRequest> findByDepartmentDepartmentIdAndStatus(Long departmentId, String status);
    List<PurchaseRequest> findByApprovedBy(String managerEmail);
    List<PurchaseRequest> findByRejectedBy(String managerEmail);
    List<PurchaseRequest> findByApprovedByOrRejectedBy(String managerEmail1, String managerEmail2);
    long countByStatus(String status);
}
