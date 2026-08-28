package com.amar.slackclone.conversation.dto;
import java.time.OffsetDateTime;
public record ConversationMessageResponse(Long id, Long conversationId, Long senderId,
        String senderDisplayName, String content, OffsetDateTime createdAt,
        OffsetDateTime updatedAt, OffsetDateTime deletedAt) {}
