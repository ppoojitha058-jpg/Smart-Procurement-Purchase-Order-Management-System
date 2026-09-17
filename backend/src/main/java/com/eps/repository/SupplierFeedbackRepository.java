package com.eps.repository;

import com.eps.entity.SupplierFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierFeedbackRepository extends JpaRepository<SupplierFeedback, Long> {
    Optional<SupplierFeedback> findByOrderOrderId(Long orderId);
    List<SupplierFeedback> findBySupplierSupplierIdOrderByCreatedDateDesc(Long supplierId);
    List<SupplierFeedback> findByUserEmailOrderByCreatedDateDesc(String email);
}
