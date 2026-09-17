package com.amar.slackclone.conversation.dto;
import java.time.OffsetDateTime;
public record ConversationReceiptRecipientResponse(Long userId,String displayName,OffsetDateTime deliveredAt,boolean read){}
