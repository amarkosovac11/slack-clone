package com.amar.slackclone.conversation.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreateConversationMessageRequest(@NotBlank @Size(max = 4000) String content) {}
