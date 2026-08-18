package com.company.kanban.controller;

import com.company.kanban.dto.NotificationResponse;
import com.company.kanban.entity.User;
import com.company.kanban.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    public NotificationController(NotificationService notificationService) { this.notificationService = notificationService; }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal User user, @RequestParam(defaultValue = "30") int limit) {
        return notificationService.list(user, limit);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal User user) { return notificationService.unreadCount(user); }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return notificationService.markRead(id, user);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal User user) { notificationService.markAllRead(user); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable Long id, @AuthenticationPrincipal User user) { notificationService.clear(id, user); }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAll(@AuthenticationPrincipal User user) { notificationService.clearAll(user); }
}
