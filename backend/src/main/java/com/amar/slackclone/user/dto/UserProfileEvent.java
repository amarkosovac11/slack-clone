package com.amar.slackclone.user.dto;

import java.time.Instant;

public record UserProfileEvent(
        String type, Long userId, String displayName, String title, String avatarUrl,
        String customStatusText, String customStatusEmoji, Instant customStatusExpiresAt,
        String presence, Instant lastSeenAt
) {}
