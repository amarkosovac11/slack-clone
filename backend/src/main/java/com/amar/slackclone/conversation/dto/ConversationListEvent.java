package com.amar.slackclone.conversation.dto;
public record ConversationListEvent(ConversationListEventType type, Long conversationId, ConversationResponse conversation) {
    public static ConversationListEvent upsert(ConversationResponse conversation) {
        return new ConversationListEvent(ConversationListEventType.UPSERT, conversation.id(), conversation);
    }
    public static ConversationListEvent removed(Long id) {
        return new ConversationListEvent(ConversationListEventType.REMOVED, id, null);
    }
}
