package com.amar.slackclone.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMessageRequest(
        @NotBlank(message = "Message content is required")
        @Size(max = 4000, message = "Message content must not exceed 4000 characters")
        String content
) {}
