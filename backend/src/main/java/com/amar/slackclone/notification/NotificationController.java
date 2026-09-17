package com.amar.slackclone.notification;

import com.amar.slackclone.notification.dto.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service){this.service=service;}
    @GetMapping public List<NotificationResponse> list(@RequestParam(defaultValue="50")int limit,Authentication a){return service.list(a.getName(),limit);}
    @GetMapping("/unread-count") public UnreadNotificationCountResponse count(Authentication a){return new UnreadNotificationCountResponse(service.unreadCount(a.getName()));}
    @PostMapping("/{id}/read") public NotificationResponse read(@PathVariable Long id,Authentication a){return service.markRead(id,a.getName());}
    @PostMapping("/read-all") public UnreadNotificationCountResponse readAll(Authentication a){return new UnreadNotificationCountResponse(service.markAllRead(a.getName()));}
}
