package com.amar.slackclone.message.dto; import java.util.List;
public record ReactionSummary(String emoji,int count,boolean reactedByCurrentUser,List<String> users){}
