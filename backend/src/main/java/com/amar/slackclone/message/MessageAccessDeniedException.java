package com.amar.slackclone.message;

public class MessageAccessDeniedException extends RuntimeException {
    public MessageAccessDeniedException() { super("Only the message sender can modify this message"); }
}
