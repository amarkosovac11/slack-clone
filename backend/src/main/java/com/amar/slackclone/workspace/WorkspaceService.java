package com.amar.slackclone.workspace;

import com.amar.slackclone.auth.InvalidCredentialsException;
import com.amar.slackclone.channel.ChannelMemberRepository;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import com.amar.slackclone.workspace.dto.CreateWorkspaceRequest;
import com.amar.slackclone.workspace.dto.WorkspaceResponse;
import com.amar.slackclone.workspace.dto.WorkspaceMemberResponse;
import com.amar.slackclone.workspace.dto.UpdateWorkspaceMemberRoleRequest;
import com.amar.slackclone.workspace.dto.UpdateWorkspaceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class WorkspaceService {


private final WorkspaceRepository workspaceRepository;
private final WorkspaceMemberRepository workspaceMemberRepository;
private final UserRepository userRepository;
private final ChannelMemberRepository channelMemberRepository;
private final WorkspaceAccessService workspaceAccessService;

public WorkspaceService(
        WorkspaceRepository workspaceRepository,
        WorkspaceMemberRepository workspaceMemberRepository,
        UserRepository userRepository,
        ChannelMemberRepository channelMemberRepository,
        WorkspaceAccessService workspaceAccessService
) {
    this.workspaceRepository = workspaceRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.userRepository = userRepository;
    this.channelMemberRepository = channelMemberRepository;
    this.workspaceAccessService = workspaceAccessService;
}

@Transactional
public WorkspaceResponse updateWorkspace(Long workspaceId, UpdateWorkspaceRequest request, String authenticatedEmail) {
    WorkspaceMember membership = workspaceAccessService.requireOwnerOrAdmin(workspaceId, authenticatedEmail);
    Workspace workspace = membership.getWorkspace();
    workspace.setName(request.name().trim());
    workspace.setUpdatedAt(Instant.now());
    return toWorkspaceResponse(membership);
}

@Transactional
public void deleteWorkspace(Long workspaceId, String authenticatedEmail) {
    WorkspaceMember owner = workspaceAccessService.requireOwner(workspaceId, authenticatedEmail);
    workspaceRepository.delete(owner.getWorkspace());
}

@Transactional
public WorkspaceResponse createWorkspace(
        CreateWorkspaceRequest request,
        String authenticatedEmail
) {
    User currentUser = userRepository
            .findByEmailIgnoreCase(authenticatedEmail)
            .orElseThrow(InvalidCredentialsException::new);

    String workspaceName = request.name().trim();
    String slug = generateUniqueSlug(workspaceName);
    Instant now = Instant.now();

    Workspace workspace = new Workspace(
            workspaceName,
            slug,
            currentUser,
            now,
            now
    );

    Workspace savedWorkspace =
            workspaceRepository.save(workspace);

    WorkspaceMember ownerMembership = new WorkspaceMember(
            savedWorkspace,
            currentUser,
            WorkspaceRole.OWNER,
            now
    );

    WorkspaceMember savedMembership =
            workspaceMemberRepository.save(ownerMembership);

    return toWorkspaceResponse(savedMembership);
}

@Transactional(readOnly = true)
public List<WorkspaceResponse> getCurrentUserWorkspaces(
        String authenticatedEmail
) {
    User currentUser = userRepository
            .findByEmailIgnoreCase(authenticatedEmail)
            .orElseThrow(InvalidCredentialsException::new);

    return workspaceMemberRepository
            .findAllByUserId(currentUser.getId())
            .stream()
            .map(this::toWorkspaceResponse)
            .toList();
}

@Transactional(readOnly = true)
public List<WorkspaceMemberResponse> getWorkspaceMembers(
        Long workspaceId,
        String authenticatedEmail
) {
    workspaceAccessService.requireWorkspaceMember(workspaceId, authenticatedEmail);

    return workspaceMemberRepository
            .findAllByWorkspaceId(workspaceId)
            .stream()
            .map(this::toWorkspaceMemberResponse)
            .toList();
}

@Transactional
public WorkspaceMemberResponse updateWorkspaceMemberRole(
        Long workspaceId,
        Long targetUserId,
        UpdateWorkspaceMemberRoleRequest request,
        String authenticatedEmail
) {
    WorkspaceMember actor = workspaceAccessService.requireOwner(workspaceId, authenticatedEmail);

    WorkspaceMember target = getTargetMembership(workspaceId, targetUserId);

    if (target.getRole() == WorkspaceRole.OWNER) {
        throw new WorkspaceMemberConflictException(
                "The workspace owner role cannot be changed"
        );
    }

    if (request.role() == WorkspaceRole.OWNER) {
        throw new WorkspaceMemberConflictException(
                "OWNER cannot be assigned through member management"
        );
    }

    target.setRole(request.role());
    return toWorkspaceMemberResponse(target);
}

@Transactional
public void removeWorkspaceMember(
        Long workspaceId,
        Long targetUserId,
        String authenticatedEmail
) {
    WorkspaceMember actor = workspaceAccessService.requireOwnerOrAdmin(workspaceId, authenticatedEmail);
    WorkspaceMember target = getTargetMembership(workspaceId, targetUserId);

    if (actor.getUser().getId().equals(targetUserId)) {
        throw new WorkspaceMemberConflictException(
                "You cannot remove yourself through member management"
        );
    }

    if (target.getRole() == WorkspaceRole.OWNER) {
        throw new WorkspaceMemberConflictException(
                "The workspace owner cannot be removed"
        );
    }

    boolean ownerCanRemove = actor.getRole() == WorkspaceRole.OWNER;
    boolean adminCanRemove = actor.getRole() == WorkspaceRole.ADMIN
            && target.getRole() == WorkspaceRole.MEMBER;

    if (!ownerCanRemove && !adminCanRemove) {
        throw new WorkspaceMemberAccessDeniedException(
                "You do not have permission to remove this workspace member"
        );
    }

    channelMemberRepository.deleteAllByWorkspaceIdAndUserId(
            workspaceId,
            targetUserId
    );
    workspaceMemberRepository.delete(target);
}

private WorkspaceMember getTargetMembership(Long workspaceId, Long userId) {
    return workspaceMemberRepository
            .findByWorkspaceIdAndUserId(workspaceId, userId)
            .orElseThrow(() -> new WorkspaceMemberNotFoundException(
                    workspaceId,
                    userId
            ));
}

private WorkspaceMemberResponse toWorkspaceMemberResponse(
        WorkspaceMember membership
) {
    User user = membership.getUser();

    return new WorkspaceMemberResponse(
            user.getId(),
            user.getDisplayName(),
            user.getEmail(),
            membership.getRole(),
            membership.getJoinedAt()
    );
}

private WorkspaceResponse toWorkspaceResponse(
        WorkspaceMember membership
) {
    Workspace workspace = membership.getWorkspace();

    return new WorkspaceResponse(
            workspace.getId(),
            workspace.getName(),
            workspace.getSlug(),
            workspace.getOwner().getId(),
            membership.getRole(),
            workspace.getCreatedAt(),
            workspace.getUpdatedAt()
    );
}

private String generateUniqueSlug(String workspaceName) {
    String baseSlug = createSlug(workspaceName);
    String candidateSlug = baseSlug;
    int suffix = 2;

    while (workspaceRepository.existsBySlug(candidateSlug)) {
        candidateSlug = baseSlug + "-" + suffix;
        suffix++;
    }

    return candidateSlug;
}

private String createSlug(String value) {
    String normalized = Normalizer
            .normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");

    String slug = normalized
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");

    if (slug.isBlank()) {
        return "workspace";
    }

    return slug;
}


}
