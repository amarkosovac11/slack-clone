package com.amar.slackclone.search.dto;
import java.time.OffsetDateTime;import java.util.List;
public record SearchPageResponse(String query,int page,int size,boolean hasNext,List<SearchHit> results){public record SearchHit(Long id,String type,String title,String snippet,String contextName,Long workspaceId,Long channelId,Long conversationId,OffsetDateTime timestamp,Long threadRootMessageId){}}
