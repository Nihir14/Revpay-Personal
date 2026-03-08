package com.revpay.service;

import com.revpay.exception.ResourceNotFoundException;
import com.revpay.model.entity.Notification;
import com.revpay.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    @Transactional
    public void createNotification(Long userId, String message, String type) {
        log.info("NOTIFICATION_CREATE_INIT | UserID: {} | Type: {}", userId, type);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setType(type);

        repository.save(notification);

        log.debug("NOTIFICATION_CREATED_SUCCESS | UserID: {} | Type: {}", userId, type);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        log.info("NOTIFICATION_FETCH_ALL | UserID: {}", userId);
        List<Notification> notifications = repository.findByUserIdOrderByCreatedAtDesc(userId);
        log.debug("Fetched {} notifications for UserID: {}", notifications.size(), userId);
        return notifications;
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotificationsPaged(Long userId, Pageable pageable) {
        log.info("NOTIFICATION_FETCH_PAGED | UserID: {}", userId);
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        log.info("NOTIFICATION_MARK_READ_INIT | NotificationID: {}", notificationId);

        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        // Idempotency: Prevent redundant database writes if already read
        if (notification.isRead()) {
            log.debug("NOTIFICATION_IDEMPOTENCY | Notification {} is already read", notificationId);
            return;
        }

        notification.setRead(true);
        repository.save(notification);

        log.info("NOTIFICATION_MARKED_READ | NotificationID: {}", notificationId);
    }
}