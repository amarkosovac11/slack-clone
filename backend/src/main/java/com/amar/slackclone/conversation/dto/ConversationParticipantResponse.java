package com.amar.slackclone.conversation.dto;
import java.time.OffsetDateTime;
public record ConversationParticipantResponse(Long userId, String displayName, String avatarUrl,
        OffsetDateTime joinedAt, String role) {}
