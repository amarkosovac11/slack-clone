package com.amar.slackclone.user.dto;

import java.time.Instant;

public record CurrentUserProfileResponse(
        Long id, String email, String displayName, String title, String avatarUrl,
        String customStatusText, String customStatusEmoji, Instant customStatusExpiresAt,
        String presence, Instant lastSeenAt, Instant createdAt
) {}
