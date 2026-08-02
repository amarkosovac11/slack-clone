package com.amar.slackclone.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository
                extends JpaRepository<WorkspaceMember, Long> {

        @EntityGraph(attributePaths = {
                        "workspace",
                        "workspace.owner"
        })
        List<WorkspaceMember> findAllByUserId(Long userId);

        List<WorkspaceMember> findAllByWorkspaceId(Long workspaceId);

        Optional<WorkspaceMember> findByWorkspaceIdAndUserId(
                        Long workspaceId,
                        Long userId);

        boolean existsByWorkspaceIdAndUserId(
                        Long workspaceId,
                        Long userId);
}