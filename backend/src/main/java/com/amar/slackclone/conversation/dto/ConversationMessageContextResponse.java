package com.amar.slackclone.conversation.dto;
import java.util.List;
public record ConversationMessageContextResponse(Long targetMessageId,Long threadRootMessageId,List<ConversationMessageResponse> messages){}
