package com.amar.slackclone.messaging.event;

import java.time.Instant;

public record MessageCreatedEvent(
        Long messageId,
        Long channelId,
        Long senderId,
        String content,
        Instant createdAt
) {
}