package com.amar.slackclone.message.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MessageResponse(

    Long id,
    Long channelId,

    Long senderId,
    String senderDisplayName,
    String senderEmail,

    String content,

    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime deletedAt,
    List<MentionResponse> mentions,
    boolean pinned

) {
}
