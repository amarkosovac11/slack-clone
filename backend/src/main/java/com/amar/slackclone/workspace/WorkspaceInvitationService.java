package com.amar.slackclone.workspace;

import com.amar.slackclone.channel.AuthenticatedUserNotFoundException;
import com.amar.slackclone.channel.UserNotFoundException;
import com.amar.slackclone.channel.WorkspaceAccessDeniedException;
import com.amar.slackclone.channel.WorkspaceNotFoundException;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import com.amar.slackclone.workspace.dto.CreateWorkspaceInvitationRequest;
import com.amar.slackclone.workspace.dto.WorkspaceInvitationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class WorkspaceInvitationService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final UserRepository userRepository;

    public WorkspaceInvitationService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceInvitationRepository workspaceInvitationRepository,
            UserRepository userRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceInvitationRepository = workspaceInvitationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WorkspaceInvitationResponse createInvitation(
            Long workspaceId,
            CreateWorkspaceInvitationRequest request,
            String authenticatedEmail
    ) {
        User currentUser = userRepository
                .findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(AuthenticatedUserNotFoundException::new);

        Workspace workspace = workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));

        WorkspaceMember currentMembership = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(
                        workspaceId,
                        currentUser.getId()
                )
                .orElseThrow(() -> new WorkspaceAccessDeniedException(
                        "You are not a member of this workspace"
                ));

        if (currentMembership.getRole() != WorkspaceRole.OWNER
                && currentMembership.getRole() != WorkspaceRole.ADMIN) {
            throw new WorkspaceAccessDeniedException(
                    "Only workspace owners and admins can invite members"
            );
        }

        if (request.role() == WorkspaceRole.OWNER) {
            throw new WorkspaceInvitationConflictException(
                    "OWNER role cannot be assigned through an invitation"
            );
        }

        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User invitedUser = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new WorkspaceInvitationConflictException(
                        "No registered user exists with this email"
                ));

        if (invitedUser.getId().equals(currentUser.getId())) {
            throw new WorkspaceInvitationConflictException(
                    "You cannot invite yourself"
            );
        }

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                workspaceId,
                invitedUser.getId()
        )) {
            throw new WorkspaceInvitationConflictException(
                    "User is already a member of this workspace"
            );
        }

        if (workspaceInvitationRepository
                .existsByWorkspaceIdAndInvitedUserIdAndStatus(
                        workspaceId,
                        invitedUser.getId(),
                        WorkspaceInvitationStatus.PENDING
                )) {
            throw new WorkspaceInvitationConflictException(
                    "A pending invitation already exists for this user"
            );
        }

        Instant now = Instant.now();

        WorkspaceInvitation invitation = new WorkspaceInvitation(
                workspace,
                invitedUser,
                currentUser,
                request.role(),
                WorkspaceInvitationStatus.PENDING,
                now,
                now.plus(7, ChronoUnit.DAYS)
        );

        WorkspaceInvitation savedInvitation =
                workspaceInvitationRepository.save(invitation);

        return toResponse(savedInvitation);
    }

    private WorkspaceInvitationResponse toResponse(
            WorkspaceInvitation invitation
    ) {
        return new WorkspaceInvitationResponse(
                invitation.getId(),

                invitation.getWorkspace().getId(),
                invitation.getWorkspace().getName(),

                invitation.getInvitedUser().getId(),
                invitation.getInvitedUser().getDisplayName(),
                invitation.getInvitedUser().getEmail(),

                invitation.getInvitedBy().getId(),
                invitation.getInvitedBy().getDisplayName(),

                invitation.getRole(),
                invitation.getStatus(),

                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt()
        );
    }
}