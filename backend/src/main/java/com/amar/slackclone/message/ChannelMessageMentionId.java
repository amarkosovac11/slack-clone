package com.amar.slackclone.message;
import java.io.Serializable;
public record ChannelMessageMentionId(Long message, Long user) implements Serializable {}
