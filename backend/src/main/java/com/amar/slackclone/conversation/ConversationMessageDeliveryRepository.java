package com.amar.slackclone.conversation;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ConversationMessageDeliveryRepository extends JpaRepository<ConversationMessageDelivery,Long>{
    boolean existsByMessageIdAndUserId(Long messageId,Long userId);
    @EntityGraph(attributePaths="user") List<ConversationMessageDelivery> findAllByMessageId(Long messageId);
    @EntityGraph(attributePaths={"user","message"}) List<ConversationMessageDelivery> findAllByMessageIdIn(Collection<Long> messageIds);
    @Query("select m from ConversationMessage m where m.conversation.id=:conversationId and m.id<=:throughId and m.sender.id<>:userId and m.createdAt>=:joinedAt")
    List<ConversationMessage> findMessagesToAcknowledge(@Param("conversationId")Long conversationId,@Param("throughId")Long throughId,@Param("userId")Long userId,@Param("joinedAt")java.time.OffsetDateTime joinedAt);
}
