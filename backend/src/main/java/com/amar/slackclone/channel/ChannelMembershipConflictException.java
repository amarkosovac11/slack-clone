package com.amar.slackclone.channel;

public class ChannelMembershipConflictException extends RuntimeException {

    public ChannelMembershipConflictException(String message) {
        super(message);
    }
}