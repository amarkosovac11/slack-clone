package com.amar.slackclone.conversation.dto;
import java.util.List;
public record ConversationMessageReceiptResponse(Long messageId,int deliveredCount,int readCount,int totalRecipients,List<ConversationReceiptRecipientResponse> recipients){}
