package com.eps.repository;

import com.eps.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByRfqRfqId(Long rfqId);
    List<Quotation> findBySupplierSupplierId(Long supplierId);
    List<Quotation> findByRfqRfqIdAndSupplierSupplierId(Long rfqId, Long supplierId);
    List<Quotation> findByStatus(String status);
}
