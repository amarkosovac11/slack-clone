package com.amar.slackclone.message.dto;
import java.time.OffsetDateTime;
public record PinnedMessageResponse(MessageResponse message,Long pinnedByUserId,String pinnedByDisplayName,OffsetDateTime pinnedAt){}
