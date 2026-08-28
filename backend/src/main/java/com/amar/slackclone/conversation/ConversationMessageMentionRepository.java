package com.amar.slackclone.conversation; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface ConversationMessageMentionRepository extends JpaRepository<ConversationMessageMention,ConversationMessageMentionId>{List<ConversationMessageMention> findAllByMessageId(Long id);void deleteAllByMessageId(Long id);}
