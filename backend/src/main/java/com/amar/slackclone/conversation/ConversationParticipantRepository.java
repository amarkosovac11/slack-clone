package com.amar.slackclone.conversation;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);
    boolean existsByConversationIdAndUserIdAndLeftAtIsNull(Long conversationId, Long userId);
    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);
    @EntityGraph(attributePaths = "user") List<ConversationParticipant> findAllByConversationId(Long conversationId);
    @EntityGraph(attributePaths = "user") List<ConversationParticipant> findAllByConversationIdAndLeftAtIsNullOrderByJoinedAt(Long conversationId);
    long countByConversationIdAndLeftAtIsNull(Long conversationId);
    @EntityGraph(attributePaths = {"user", "lastReadMessage"})
    List<ConversationParticipant> findAllByConversationIdIn(Collection<Long> conversationIds);

    @Query("select cp.user.id from ConversationParticipant cp where cp.conversation.id = :conversationId and cp.leftAt is null")
    List<Long> findUserIds(@Param("conversationId") Long conversationId);
}
