package com.amar.slackclone.conversation.dto;
import java.time.OffsetDateTime;
import java.util.List;
import com.amar.slackclone.message.dto.MentionResponse;
public record ConversationMessageResponse(Long id, Long conversationId, Long senderId,
        String senderDisplayName, String content, OffsetDateTime createdAt,
        OffsetDateTime updatedAt, OffsetDateTime deletedAt, List<MentionResponse> mentions) {}
