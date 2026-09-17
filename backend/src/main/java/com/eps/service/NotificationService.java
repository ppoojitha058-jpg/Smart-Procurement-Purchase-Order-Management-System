package com.eps.service;

import com.eps.entity.Notification;

import java.util.List;

public interface NotificationService {
    Notification sendNotification(String recipientEmail, String recipientRole, String title, String message);
    List<Notification> getNotifications(String email, String role);
    Notification markAsRead(Long notificationId);
    void markAllAsRead(String email, String role);
}
