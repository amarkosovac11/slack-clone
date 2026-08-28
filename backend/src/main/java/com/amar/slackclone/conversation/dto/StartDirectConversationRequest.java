package com.amar.slackclone.conversation.dto;
import jakarta.validation.constraints.NotNull;
public record StartDirectConversationRequest(@NotNull Long userId) {}
