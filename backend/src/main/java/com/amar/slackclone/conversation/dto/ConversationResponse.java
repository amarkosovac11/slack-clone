package com.amar.slackclone.conversation.dto;
import com.amar.slackclone.conversation.ConversationType;
import java.time.OffsetDateTime;
import java.util.List;
public record ConversationResponse(Long id, ConversationType type, List<ConversationUserResponse> participants,
        String displayName, ConversationMessageResponse lastMessage, long unreadCount,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
