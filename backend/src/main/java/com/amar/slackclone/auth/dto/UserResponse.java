package com.amar.slackclone.auth.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        Instant createdAt
) {
}