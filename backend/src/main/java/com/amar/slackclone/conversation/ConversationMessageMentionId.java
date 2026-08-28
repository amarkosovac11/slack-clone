package com.amar.slackclone.conversation; import java.io.Serializable;
public record ConversationMessageMentionId(Long message,Long user) implements Serializable{}
