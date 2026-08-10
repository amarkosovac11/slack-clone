package com.amar.slackclone.channel;

public class ChannelNotFoundException extends RuntimeException {

    public ChannelNotFoundException(Long channelId) {
        super("Channel with id " + channelId + " was not found");
    }
}
