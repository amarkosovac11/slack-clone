package com.amar.slackclone.channel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChannelMemberRepository
        extends JpaRepository<ChannelMember, Long> {

    boolean existsByChannelIdAndUserId(
            Long channelId,
            Long userId
    );

    Optional<ChannelMember> findByChannelIdAndUserId(
            Long channelId,
            Long userId
    );

    @EntityGraph(attributePaths = "user")
    List<ChannelMember> findAllByChannelId(
            Long channelId
    );

    List<ChannelMember> findAllByUserId(
            Long userId
    );

    @Modifying
    @Query("""
            delete from ChannelMember membership
            where membership.user.id = :userId
              and membership.channel.workspace.id = :workspaceId
            """)
    int deleteAllByWorkspaceIdAndUserId(
            @Param("workspaceId") Long workspaceId,
            @Param("userId") Long userId
    );
}
