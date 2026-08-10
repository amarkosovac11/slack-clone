package com.amar.slackclone.channel.dto;

import jakarta.validation.constraints.NotNull;

public record AddChannelMemberRequest(

        @NotNull(message = "User id is required")
        Long userId

) {
}