package com.amar.slackclone.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChannelRequest(
        @NotBlank(message = "Channel name is required")
        @Size(min = 2, max = 100, message = "Channel name must be between 2 and 100 characters")
        String name,
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {}
