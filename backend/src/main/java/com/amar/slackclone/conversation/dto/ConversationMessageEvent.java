package com.amar.slackclone.conversation.dto;
public record ConversationMessageEvent(ConversationMessageEventType type, ConversationMessageResponse message) {}
