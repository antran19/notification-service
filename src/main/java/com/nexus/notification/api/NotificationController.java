package com.nexus.notification.api;

import com.nexus.notification.api.dto.NotificationResponse;
import com.nexus.notification.api.mapper.NotificationApiMapper;
import com.nexus.notification.application.usecase.ListAllNotificationsUseCase;
import com.nexus.notification.application.usecase.ListMyNotificationsUseCase;
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
    private final NotificationApiMapper mapper;

    public NotificationController(ListMyNotificationsUseCase listMyNotificationsUseCase,
                                   ListAllNotificationsUseCase listAllNotificationsUseCase,
                                   MarkNotificationReadUseCase markNotificationReadUseCase,
                                   NotificationApiMapper mapper) {
        this.listMyNotificationsUseCase = listMyNotificationsUseCase;
        this.listAllNotificationsUseCase = listAllNotificationsUseCase;
        this.markNotificationReadUseCase = markNotificationReadUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listMine(Authentication authentication) {
        List<NotificationResponse> results = listMyNotificationsUseCase.list(callerId(authentication))
                .stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(results));
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
