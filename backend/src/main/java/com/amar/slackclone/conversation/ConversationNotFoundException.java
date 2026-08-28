package com.amar.slackclone.conversation;
public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException(Long id) { super("Conversation not found: " + id); }
}
