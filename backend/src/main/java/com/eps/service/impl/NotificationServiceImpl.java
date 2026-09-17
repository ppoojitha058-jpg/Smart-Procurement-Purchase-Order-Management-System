package com.eps.service.impl;

import com.eps.entity.Notification;
import com.eps.repository.NotificationRepository;
import com.eps.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Notification sendNotification(String recipientEmail, String recipientRole, String title, String message) {
        Notification notification = new Notification();
        notification.setRecipientEmail(recipientEmail);
        notification.setRecipientRole(recipientRole);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreatedDate(LocalDateTime.now());
        log.info("Sending notification: [{}] to email: {}, role: {}", title, recipientEmail, recipientRole);
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getNotifications(String email, String role) {
        if (email != null && role != null) {
            List<Notification> notifications = new ArrayList<>(notificationRepository.findByRecipientEmailOrderByCreatedDateDesc(email));
            notifications.addAll(notificationRepository.findByRecipientRoleOrderByCreatedDateDesc(role));
            return notifications.stream().distinct()
                    .sorted(Comparator.comparing(Notification::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } else if (email != null) {
            return notificationRepository.findByRecipientEmailOrderByCreatedDateDesc(email);
        } else if (role != null) {
            return notificationRepository.findByRecipientRoleOrderByCreatedDateDesc(role);
        }
        return notificationRepository.findAll();
    }

    @Override
    public Notification markAsRead(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .map(n -> {
                    n.setIsRead(true);
                    return notificationRepository.save(n);
                })
                .orElse(null);
    }

    @Override
    public void markAllAsRead(String email, String role) {
        List<Notification> list = getNotifications(email, role);
        for (Notification n : list) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(list);
    }
}
