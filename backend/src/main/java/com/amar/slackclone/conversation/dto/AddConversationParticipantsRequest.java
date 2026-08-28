package com.amar.slackclone.conversation.dto;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
public record AddConversationParticipantsRequest(@NotEmpty List<Long> userIds) {}
