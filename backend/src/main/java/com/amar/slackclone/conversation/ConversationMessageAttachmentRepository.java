package com.amar.slackclone.conversation;import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface ConversationMessageAttachmentRepository extends JpaRepository<ConversationMessageAttachment,Long>{List<ConversationMessageAttachment> findAllByMessageId(Long id);}
