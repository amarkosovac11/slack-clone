package com.amar.slackclone.conversation.dto;
public record ConversationReceiptEvent(String type,Long conversationId,Long actorUserId,Long throughMessageId,ConversationMessageReceiptResponse receipt){}
