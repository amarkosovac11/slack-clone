package com.amar.slackclone.workspace;

import com.amar.slackclone.channel.AuthenticatedUserNotFoundException;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import com.amar.slackclone.workspace.dto.CreateWorkspaceInvitationRequest;
import com.amar.slackclone.workspace.dto.WorkspaceInvitationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.List;

@Service
public class WorkspaceInvitationService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public WorkspaceInvitationService(
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceInvitationRepository workspaceInvitationRepository,
            UserRepository userRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceInvitationRepository = workspaceInvitationRepository;
        this.userRepository = userRepository;
        this.workspaceAccessService = workspaceAccessService;
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

        Workspace workspace = workspaceAccessService
                .requireOwnerOrAdmin(workspaceId, authenticatedEmail).getWorkspace();

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

        Instant now = Instant.now();

        workspaceInvitationRepository
                .findByWorkspaceIdAndInvitedUserIdAndStatus(
                        workspaceId,
                        invitedUser.getId(),
                        WorkspaceInvitationStatus.PENDING
                )
                .ifPresent(existingInvitation -> {
                    if (isExpired(existingInvitation, now)) {
                        existingInvitation.setStatus(
                                WorkspaceInvitationStatus.EXPIRED
                        );
                        workspaceInvitationRepository.saveAndFlush(
                                existingInvitation
                        );
                    } else {
                        throw new WorkspaceInvitationConflictException(
                                "A pending invitation already exists for this user"
                        );
                    }
                });

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

    @Transactional
    public List<WorkspaceInvitationResponse> getCurrentUserPendingInvitations(
            String authenticatedEmail
    ) {
        User currentUser = getAuthenticatedUser(authenticatedEmail);
        Instant now = Instant.now();

        return workspaceInvitationRepository
                .findAllByInvitedUserIdAndStatusOrderByCreatedAtDesc(
                        currentUser.getId(),
                        WorkspaceInvitationStatus.PENDING
                )
                .stream()
                .filter(invitation -> expireIfNecessary(invitation, now))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<WorkspaceInvitationResponse> getWorkspaceInvitations(
            Long workspaceId,
            String authenticatedEmail
    ) {
        User currentUser = getAuthenticatedUser(authenticatedEmail);

        workspaceAccessService.requireOwnerOrAdmin(workspaceId, authenticatedEmail);

        Instant now = Instant.now();

        List<WorkspaceInvitation> invitations = workspaceInvitationRepository
                .findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId);

        invitations.forEach(invitation -> expireIfNecessary(invitation, now));

        return invitations.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(noRollbackFor = WorkspaceInvitationConflictException.class)
    public WorkspaceInvitationResponse acceptInvitation(
            Long invitationId,
            String authenticatedEmail
    ) {
        User currentUser = getAuthenticatedUser(authenticatedEmail);
        WorkspaceInvitation invitation = getInvitationForUser(
                invitationId,
                currentUser
        );
        Instant now = Instant.now();

        requireActionable(invitation, now);

        if (invitation.getRole() == WorkspaceRole.OWNER) {
            throw new WorkspaceInvitationConflictException(
                    "OWNER role cannot be assigned through an invitation"
            );
        }

        Long workspaceId = invitation.getWorkspace().getId();

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                workspaceId,
                currentUser.getId()
        )) {
            throw new WorkspaceInvitationConflictException(
                    "You are already a member of this workspace"
            );
        }

        WorkspaceMember membership = new WorkspaceMember(
                invitation.getWorkspace(),
                currentUser,
                invitation.getRole(),
                now
        );

        workspaceMemberRepository.save(membership);
        invitation.setStatus(WorkspaceInvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(now);

        return toResponse(invitation);
    }

    @Transactional(noRollbackFor = WorkspaceInvitationConflictException.class)
    public WorkspaceInvitationResponse rejectInvitation(
            Long invitationId,
            String authenticatedEmail
    ) {
        User currentUser = getAuthenticatedUser(authenticatedEmail);
        WorkspaceInvitation invitation = getInvitationForUser(
                invitationId,
                currentUser
        );
        Instant now = Instant.now();

        requireActionable(invitation, now);
        invitation.setStatus(WorkspaceInvitationStatus.REJECTED);

        return toResponse(invitation);
    }

    private User getAuthenticatedUser(String authenticatedEmail) {
        return userRepository
                .findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(AuthenticatedUserNotFoundException::new);
    }

    private WorkspaceInvitation getInvitationForUser(
            Long invitationId,
            User currentUser
    ) {
        WorkspaceInvitation invitation = workspaceInvitationRepository
                .findById(invitationId)
                .orElseThrow(() -> new WorkspaceInvitationNotFoundException(
                        invitationId
                ));

        if (!invitation.getInvitedUser().getId().equals(currentUser.getId())) {
            throw new WorkspaceInvitationAccessDeniedException(
                    "This invitation belongs to another user"
            );
        }

        return invitation;
    }

    private void requireActionable(
            WorkspaceInvitation invitation,
            Instant now
    ) {
        if (invitation.getStatus() == WorkspaceInvitationStatus.EXPIRED) {
            throw new WorkspaceInvitationConflictException(
                    "Invitation has expired"
            );
        }

        if (invitation.getStatus() != WorkspaceInvitationStatus.PENDING) {
            throw new WorkspaceInvitationConflictException(
                    "Invitation is no longer pending"
            );
        }

        if (isExpired(invitation, now)) {
            invitation.setStatus(WorkspaceInvitationStatus.EXPIRED);
            throw new WorkspaceInvitationConflictException(
                    "Invitation has expired"
            );
        }
    }

    private boolean expireIfNecessary(
            WorkspaceInvitation invitation,
            Instant now
    ) {
        if (invitation.getStatus() == WorkspaceInvitationStatus.PENDING
                && isExpired(invitation, now)) {
            invitation.setStatus(WorkspaceInvitationStatus.EXPIRED);
            return false;
        }

        return true;
    }

    private boolean isExpired(
            WorkspaceInvitation invitation,
            Instant now
    ) {
        return !invitation.getExpiresAt().isAfter(now);
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
