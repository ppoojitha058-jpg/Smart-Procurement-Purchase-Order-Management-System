package com.eps.repository;

import com.eps.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientEmailOrderByCreatedDateDesc(String recipientEmail);
    List<Notification> findByRecipientRoleOrderByCreatedDateDesc(String recipientRole);
    List<Notification> findByRecipientEmailAndRecipientRoleOrderByCreatedDateDesc(String recipientEmail, String recipientRole);
}
