package com.amar.slackclone.message.dto;
import java.util.List;
public record MessageContextResponse(Long targetMessageId,Long threadRootMessageId,List<MessageResponse> messages){}
