package com.amar.slackclone.conversation;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByDirectKey(String directKey);

    @EntityGraph(attributePaths = "createdBy")
    @Query("select cp.conversation from ConversationParticipant cp where cp.user.id = :userId and cp.leftAt is null and cp.hiddenAt is null order by cp.conversation.updatedAt desc")
    List<Conversation> findAllForUser(@Param("userId") Long userId);
    @EntityGraph(attributePaths="createdBy")
    @Query("select cp.conversation from ConversationParticipant cp where cp.user.id=:userId and cp.leftAt is null and cp.hiddenAt is not null order by cp.hiddenAt desc")
    List<Conversation> findHiddenForUser(@Param("userId") Long userId);

    @Query(value = """
        INSERT INTO conversations(type, created_by, direct_key, created_at, updated_at)
        VALUES ('DIRECT', :creatorId, :directKey, NOW(), NOW())
        ON CONFLICT (direct_key) DO NOTHING RETURNING id
        """, nativeQuery = true)
    Optional<Long> insertDirect(@Param("creatorId") Long creatorId, @Param("directKey") String directKey);
}
