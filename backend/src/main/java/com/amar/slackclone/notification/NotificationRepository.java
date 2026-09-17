package com.amar.slackclone.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @EntityGraph(attributePaths={"recipient","actor"})
    List<Notification> findAllByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    @EntityGraph(attributePaths={"recipient","actor"})
    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);
    long countByRecipientIdAndReadAtIsNull(Long recipientId);
    @Modifying @Query("update Notification n set n.readAt=:readAt where n.recipient.id=:recipientId and n.readAt is null")
    int markAllRead(@Param("recipientId") Long recipientId, @Param("readAt") OffsetDateTime readAt);
}
