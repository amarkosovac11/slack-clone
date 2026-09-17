package com.amar.slackclone.message.dto;

public record ChannelMessageEvent(
    ChannelMessageEventType type,
    MessageResponse message,
    Long threadRootMessageId
) {}
