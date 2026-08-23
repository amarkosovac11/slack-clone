package com.amar.slackclone.message.dto;

import java.time.OffsetDateTime;

public record MessageResponse(

    Long id,
    Long channelId,

    Long senderId,
    String senderDisplayName,
    String senderEmail,

    String content,

    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime deletedAt

) {
}
