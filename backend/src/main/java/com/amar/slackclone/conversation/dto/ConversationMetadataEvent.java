package com.amar.slackclone.conversation.dto;
public record ConversationMetadataEvent(String type, Long conversationId, Long affectedUserId) {}
