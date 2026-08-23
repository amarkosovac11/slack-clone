package com.amar.slackclone.channel.dto;

import java.time.OffsetDateTime;

public record ChannelResponse(
    Long id,
    Long workspaceId,
    String name,
    String slug,
    String description,
    boolean privateChannel,
    Long createdById,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime archivedAt
) {
}
