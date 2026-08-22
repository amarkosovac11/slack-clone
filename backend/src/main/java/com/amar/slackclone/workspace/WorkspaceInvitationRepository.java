package com.amar.slackclone.workspace;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceInvitationRepository
        extends JpaRepository<WorkspaceInvitation, Long> {

    boolean existsByWorkspaceIdAndInvitedUserIdAndStatus(
            Long workspaceId,
            Long invitedUserId,
            WorkspaceInvitationStatus status
    );

    Optional<WorkspaceInvitation> findByWorkspaceIdAndInvitedUserIdAndStatus(
            Long workspaceId,
            Long invitedUserId,
            WorkspaceInvitationStatus status
    );

    @EntityGraph(attributePaths = {
            "workspace",
            "invitedUser",
            "invitedBy"
    })
    List<WorkspaceInvitation> findAllByInvitedUserIdAndStatusOrderByCreatedAtDesc(
            Long invitedUserId,
            WorkspaceInvitationStatus status
    );

    @EntityGraph(attributePaths = {
            "workspace",
            "invitedUser",
            "invitedBy"
    })
    List<WorkspaceInvitation> findAllByWorkspaceIdOrderByCreatedAtDesc(
            Long workspaceId
    );

    @EntityGraph(attributePaths = {
            "workspace",
            "invitedUser",
            "invitedBy"
    })
    Optional<WorkspaceInvitation> findByIdAndInvitedUserId(
            Long id,
            Long invitedUserId
    );

    @Override
    @EntityGraph(attributePaths = {
            "workspace",
            "invitedUser",
            "invitedBy"
    })
    Optional<WorkspaceInvitation> findById(Long id);
}
