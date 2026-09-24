package com.amar.slackclone.messaging.event;

import com.amar.slackclone.message.dto.MessageResponse;

/** Saved message snapshot, plus the updated root snapshot for a thread reply. */
public record MessageCreatedEvent(
        Long workspaceId,
        MessageResponse message,
        MessageResponse threadRoot
) {
}
