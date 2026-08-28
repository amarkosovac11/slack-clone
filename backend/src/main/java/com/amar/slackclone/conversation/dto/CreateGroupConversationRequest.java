package com.amar.slackclone.conversation.dto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
public record CreateGroupConversationRequest(
        @NotEmpty @Size(max = 50, message = "Too many participant entries") List<Long> participantIds
) {}
