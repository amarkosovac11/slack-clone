package com.amar.slackclone.user.dto;

import java.time.Instant;

public record UserSummaryResponse(
        Long id, String displayName, String username, String title, String avatarUrl,
        String customStatusText, String customStatusEmoji, Instant customStatusExpiresAt,
        String presence, Instant lastSeenAt
) {}
