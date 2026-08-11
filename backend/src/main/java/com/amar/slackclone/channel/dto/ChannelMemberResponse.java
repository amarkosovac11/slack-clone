package com.amar.slackclone.channel.dto;

import java.time.OffsetDateTime;

public record ChannelMemberResponse(
        Long userId,
        String displayName,
        String email,
        OffsetDateTime joinedAt
) {
}
