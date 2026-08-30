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
    @EntityGraph(attributePaths = "sender")
    List<ConversationMessage> findByConversationIdAndCreatedAtGreaterThanEqualOrderByIdDesc(Long conversationId, java.time.OffsetDateTime joinedAt, Pageable page);
    @EntityGraph(attributePaths = "sender")
    List<ConversationMessage> findByConversationIdAndIdLessThanAndCreatedAtGreaterThanEqualOrderByIdDesc(Long conversationId, Long before, java.time.OffsetDateTime joinedAt, Pageable page);
    Optional<ConversationMessage> findTopByConversationIdOrderByIdDesc(Long conversationId);
    Optional<ConversationMessage> findTopByConversationIdAndCreatedAtGreaterThanEqualOrderByIdDesc(Long conversationId, java.time.OffsetDateTime joinedAt);
    Optional<ConversationMessage> findByIdAndConversationId(Long id, Long conversationId);
    List<ConversationMessage> findAllByThreadRootMessageIdAndCreatedAtGreaterThanEqualOrderByCreatedAt(Long rootId,java.time.OffsetDateTime joinedAt);
    long countByThreadRootMessageId(Long rootId);
    @Query("select m from ConversationMessage m join ConversationParticipant cp on cp.conversation=m.conversation where cp.user.id=:userId and cp.leftAt is null and m.createdAt>=cp.joinedAt and m.deletedAt is null and lower(m.content) like lower(concat('%',:q,'%')) order by m.createdAt desc")List<ConversationMessage> searchAccessible(@Param("userId")Long userId,@Param("q")String q,Pageable p);

    @Query("select count(m) from ConversationMessage m where m.conversation.id = :conversationId and m.id > :afterId and m.sender.id <> :userId and m.deletedAt is null")
    long countUnread(@Param("conversationId") Long conversationId, @Param("afterId") Long afterId, @Param("userId") Long userId);

    @Query("select count(m) from ConversationMessage m where m.conversation.id = :conversationId and m.createdAt >= :joinedAt and m.id > :afterId and m.sender.id <> :userId and m.deletedAt is null")
    long countUnreadSince(@Param("conversationId") Long conversationId, @Param("joinedAt") java.time.OffsetDateTime joinedAt,
            @Param("afterId") Long afterId, @Param("userId") Long userId);

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
