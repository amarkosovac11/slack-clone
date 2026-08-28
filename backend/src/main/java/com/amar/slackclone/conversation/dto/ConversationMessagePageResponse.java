package com.amar.slackclone.conversation.dto;
import java.util.List;
public record ConversationMessagePageResponse(List<ConversationMessageResponse> messages, Long nextBefore) {}
