package com.eps.repository;

import com.eps.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    List<GoodsReceipt> findByPurchaseOrderOrderId(Long orderId);
    Optional<GoodsReceipt> findFirstByPurchaseOrderOrderIdOrderByReceivedDateDesc(Long orderId);
}
