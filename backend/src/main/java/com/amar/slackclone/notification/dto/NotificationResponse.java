package com.amar.slackclone.notification.dto;

import com.amar.slackclone.notification.NotificationType;
import java.time.OffsetDateTime;

public record NotificationResponse(Long id, NotificationType type, Long actorId, String actorDisplayName,
    String actorAvatarUrl, Long workspaceId, Long channelId, Long conversationId, Long channelMessageId,
    Long conversationMessageId, String text, OffsetDateTime createdAt, OffsetDateTime readAt) {}
