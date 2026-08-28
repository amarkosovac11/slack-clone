package com.amar.slackclone.conversation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    interface UnreadCount { Long getConversationId(); long getUnreadCount(); }
    @EntityGraph(attributePaths = "sender")
    List<ConversationMessage> findByConversationIdAndIdLessThanOrderByIdDesc(Long conversationId, Long before, Pageable page);
    @EntityGraph(attributePaths = "sender")
    List<ConversationMessage> findByConversationIdOrderByIdDesc(Long conversationId, Pageable page);
    Optional<ConversationMessage> findTopByConversationIdOrderByIdDesc(Long conversationId);
    Optional<ConversationMessage> findByIdAndConversationId(Long id, Long conversationId);

    @Query("select count(m) from ConversationMessage m where m.conversation.id = :conversationId and m.id > :afterId and m.sender.id <> :userId and m.deletedAt is null")
    long countUnread(@Param("conversationId") Long conversationId, @Param("afterId") Long afterId, @Param("userId") Long userId);

    @EntityGraph(attributePaths = "sender")
    @Query("""
        select m from ConversationMessage m where m.conversation.id in :conversationIds
        and m.id = (select max(latest.id) from ConversationMessage latest where latest.conversation.id = m.conversation.id)
        """)
    List<ConversationMessage> findLatest(@Param("conversationIds") Collection<Long> conversationIds);

    @Query("""
        select cp.conversation.id as conversationId, count(m.id) as unreadCount
        from ConversationParticipant cp left join ConversationMessage m
          on m.conversation = cp.conversation and m.id > coalesce(cp.lastReadMessage.id, 0) and m.sender.id <> :userId and m.deletedAt is null
        where cp.user.id = :userId group by cp.conversation.id
        """)
    List<UnreadCount> findUnreadCounts(@Param("userId") Long userId);
}
