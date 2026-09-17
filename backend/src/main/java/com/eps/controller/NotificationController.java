package com.eps.controller;

import com.eps.entity.Notification;
import com.eps.service.NotificationService;
import com.eps.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        String role = (authentication != null && !authentication.getAuthorities().isEmpty()) ?
                authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "") : null;
        return ResponseEntity.ok(notificationService.getNotifications(email, role));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(@PathVariable Long id, Authentication authentication) {
        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null) return ResponseEntity.notFound().build();
        if (!canAccess(notification, authentication)) return ResponseEntity.status(403).build();
        Notification updated = notificationService.markAsRead(id);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        String role = (authentication != null && !authentication.getAuthorities().isEmpty()) ?
                authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "") : null;
        notificationService.markAllAsRead(email, role);
        return ResponseEntity.ok().build();
    }

    private boolean canAccess(Notification notification, Authentication authentication) {
        if (authentication == null) return false;
        String email = authentication.getName();
        String role = authentication.getAuthorities().isEmpty() ? null
                : authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return (notification.getRecipientEmail() != null && notification.getRecipientEmail().equalsIgnoreCase(email))
                || (notification.getRecipientEmail() == null && notification.getRecipientRole() != null
                && notification.getRecipientRole().equalsIgnoreCase(role));
    }
}
