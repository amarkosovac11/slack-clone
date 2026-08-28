package com.amar.slackclone.conversation.dto; import jakarta.validation.constraints.NotNull;
public record TransferConversationCreatorRequest(@NotNull Long newCreatorUserId){}
