package com.amar.slackclone.channel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    List<Channel> findAllByWorkspaceIdOrderByCreatedAtAsc(Long workspaceId);

    Optional<Channel> findByWorkspaceIdAndSlug(Long workspaceId, String slug);

    boolean existsByWorkspaceIdAndSlug(Long workspaceId, String slug);
}