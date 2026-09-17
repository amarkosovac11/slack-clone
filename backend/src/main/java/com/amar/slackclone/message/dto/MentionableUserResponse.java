package com.amar.slackclone.message.dto;

public record MentionableUserResponse(Long userId, String displayName, String username, String avatarUrl) {}
