package com.amar.slackclone.channel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    List<Channel> findAllByWorkspaceIdOrderByCreatedAtAsc(Long workspaceId);
    List<Channel> findAllByWorkspaceIdAndArchivedAtIsNotNullOrderByArchivedAtDesc(Long workspaceId);

    Optional<Channel> findByWorkspaceIdAndSlug(Long workspaceId, String slug);

    boolean existsByWorkspaceIdAndSlug(Long workspaceId, String slug);

    boolean existsByWorkspaceIdAndSlugAndIdNot(Long workspaceId, String slug, Long id);

    Optional<Channel> findByIdAndWorkspaceId(
        Long channelId,
        Long workspaceId
);

    @Query("""
        SELECT DISTINCT c
        FROM Channel c
        LEFT JOIN ChannelMember cm
            ON cm.channel = c
            AND cm.user.id = :userId
        WHERE c.workspace.id = :workspaceId
          AND c.archivedAt IS NULL
          AND (
              c.privateChannel = false
              OR cm.id IS NOT NULL
          )
        ORDER BY c.createdAt ASC
        """)
    List<Channel> findVisibleChannels(
            @Param("workspaceId") Long workspaceId,
            @Param("userId") Long userId
    );
}
