package com.amar.slackclone.message;

public class MessageNotFoundException extends RuntimeException {
    public MessageNotFoundException(Long messageId) { super("Message not found: " + messageId); }
}
