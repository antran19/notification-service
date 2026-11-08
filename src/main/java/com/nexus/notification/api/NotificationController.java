package com.nexus.notification.api;

import com.nexus.notification.api.dto.MarkAllReadResponse;
import com.nexus.notification.api.dto.NotificationResponse;
import com.nexus.notification.api.dto.UnreadCountResponse;
import com.nexus.notification.api.mapper.NotificationApiMapper;
import com.nexus.notification.application.usecase.CountMyUnreadNotificationsUseCase;
import com.nexus.notification.application.usecase.ListAllNotificationsUseCase;
import com.nexus.notification.application.usecase.ListMyNotificationsUseCase;
import com.nexus.notification.application.usecase.MarkAllNotificationsReadUseCase;
import com.nexus.notification.application.usecase.MarkNotificationReadUseCase;
import com.nexus.common.core.ApiResponse;
import com.nexus.common.security.RequiresPrivilege;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final ListMyNotificationsUseCase listMyNotificationsUseCase;
    private final ListAllNotificationsUseCase listAllNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final CountMyUnreadNotificationsUseCase countMyUnreadNotificationsUseCase;
    private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
    private final NotificationApiMapper mapper;

    public NotificationController(ListMyNotificationsUseCase listMyNotificationsUseCase,
                                   ListAllNotificationsUseCase listAllNotificationsUseCase,
                                   MarkNotificationReadUseCase markNotificationReadUseCase,
                                   CountMyUnreadNotificationsUseCase countMyUnreadNotificationsUseCase,
                                   MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase,
                                   NotificationApiMapper mapper) {
        this.listMyNotificationsUseCase = listMyNotificationsUseCase;
        this.listAllNotificationsUseCase = listAllNotificationsUseCase;
        this.markNotificationReadUseCase = markNotificationReadUseCase;
        this.countMyUnreadNotificationsUseCase = countMyUnreadNotificationsUseCase;
        this.markAllNotificationsReadUseCase = markAllNotificationsReadUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listMine(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<NotificationResponse> results = listMyNotificationsUseCase.list(callerId(authentication), unread, page, size)
                .stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> unreadCount(Authentication authentication) {
        long count = countMyUnreadNotificationsUseCase.count(callerId(authentication));
        return ResponseEntity.ok(ApiResponse.ok(new UnreadCountResponse(count)));
    }

    @PatchMapping("/me/read-all")
    public ResponseEntity<ApiResponse<MarkAllReadResponse>> markAllRead(Authentication authentication) {
        int updated = markAllNotificationsReadUseCase.markAllRead(callerId(authentication));
        return ResponseEntity.ok(ApiResponse.ok(new MarkAllReadResponse(updated)));
    }

    @RequiresPrivilege("NOTIFICATION.AUDIT")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listAll(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String aggregateId) {
        List<NotificationResponse> results = listAllNotificationsUseCase.list(eventType, aggregateId)
                .stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(Authentication authentication, @PathVariable String id) {
        markNotificationReadUseCase.markRead(id, callerId(authentication));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private static String callerId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }
}
