package com.amar.slackclone.conversation.dto;
import jakarta.validation.constraints.Size;
public record UpdateConversationRequest(@Size(max = 100, message = "Group name cannot exceed 100 characters") String name) {}
