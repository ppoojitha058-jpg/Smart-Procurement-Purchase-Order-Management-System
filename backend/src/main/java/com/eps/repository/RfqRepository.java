package com.eps.repository;

import com.eps.entity.Rfq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RfqRepository extends JpaRepository<Rfq, Long> {
    List<Rfq> findByStatus(String status);
    List<Rfq> findByPurchaseRequestRequestId(Long requestId);
}
