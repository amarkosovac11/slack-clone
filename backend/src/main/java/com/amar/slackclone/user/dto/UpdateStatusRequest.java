package com.amar.slackclone.user.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateStatusRequest(
        @Size(max = 100) String text,
        @Size(max = 32) String emoji,
        Instant expiresAt
) {}
