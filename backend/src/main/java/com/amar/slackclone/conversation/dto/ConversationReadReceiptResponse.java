package com.amar.slackclone.conversation.dto; import java.util.List;
public record ConversationReadReceiptResponse(Long messageId,int readCount,int totalEligibleReaders,List<String> readerNames){}
