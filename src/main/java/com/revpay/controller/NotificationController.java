package com.revpay.controller;

import com.revpay.model.dto.ApiResponse;
import com.revpay.model.entity.Notification;
import com.revpay.security.UserDetailsImpl;
import com.revpay.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // Secures endpoints for all logged-in users
@Tag(name = "Notifications", description = "Endpoints for retrieving and managing user inbox notifications and system alerts")
public class NotificationController {

    private final NotificationService service;

    // --- DTOs ---
    public record NotificationDTO(Long id, String message, String type, boolean isRead, LocalDateTime createdAt) {}

    // NEW: DTO to handle user preferences
    public record NotificationPreferencesDTO(boolean emailAlerts, boolean pushNotifications, Map<String, Boolean> typePreferences) {}

    // Helper method to extract user ID without hitting the database!
    private Long getUserId(Authentication auth) {
        return ((UserDetailsImpl) auth.getPrincipal()).getUserId();
    }

    // --- ENDPOINTS ---

    @GetMapping
    @Operation(summary = "Get user notifications", description = "Retrieves a paginated list of the authenticated user's notifications, sorted by most recent.")
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getMyNotifications(
            Authentication auth,
            @PageableDefault(size = 20) Pageable pageable) {

        Long userId = getUserId(auth);
        log.debug("Fetching notifications for User ID: {}. Page: {}, Size: {}", userId, pageable.getPageNumber(), pageable.getPageSize());

        Page<Notification> notifications = service.getUserNotificationsPaged(userId, pageable);

        Page<NotificationDTO> dtos = notifications.map(n -> new NotificationDTO(
                n.getId(),
                n.getMessage(),
                n.getType(),
                n.isRead(),
                n.getCreatedAt()
        ));

        return ResponseEntity.ok(ApiResponse.success(dtos, "Notifications retrieved successfully"));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Updates a specific notification's status to read.")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @Parameter(description = "ID of the notification to mark as read") @PathVariable Long id) {

        log.info("Marking Notification ID: {} as read", id);
        service.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }

    // ---> NEW PREFERENCE ENDPOINTS <---

    @GetMapping("/preferences")
    @Operation(summary = "Get notification preferences", description = "Retrieves the user's current notification settings (e.g., email alerts, specific types).")
    public ResponseEntity<ApiResponse<NotificationPreferencesDTO>> getPreferences(Authentication auth) {
        // TODO: Implement service.getPreferences(getUserId(auth)) in Phase 2
        return ResponseEntity.ok(ApiResponse.success(null, "Preferences retrieved successfully (Needs Service Logic)"));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update notification preferences", description = "Allows the user to enable/disable specific notification channels and types.")
    public ResponseEntity<ApiResponse<String>> updatePreferences(
            @RequestBody NotificationPreferencesDTO preferences,
            Authentication auth) {

        // TODO: Implement service.updatePreferences(getUserId(auth), preferences) in Phase 2
        return ResponseEntity.ok(ApiResponse.success(null, "Notification preferences updated successfully (Needs Service Logic)"));
    }

    @PostMapping("/test")
    @Operation(summary = "Generate test notification", description = "Creates a sample system notification for the authenticated user to test delivery.")
    public ResponseEntity<ApiResponse<String>> testNotification(Authentication auth) {

        Long userId = getUserId(auth);
        service.createNotification(userId, "Test notification from RevPay System", "SYSTEM");

        return ResponseEntity.ok(ApiResponse.success(null, "Test notification created successfully"));
    }
}