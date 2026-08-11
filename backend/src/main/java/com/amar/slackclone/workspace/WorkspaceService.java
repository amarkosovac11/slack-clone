package com.amar.slackclone.workspace;

import com.amar.slackclone.auth.InvalidCredentialsException;
import com.amar.slackclone.channel.AuthenticatedUserNotFoundException;
import com.amar.slackclone.channel.WorkspaceAccessDeniedException;
import com.amar.slackclone.channel.WorkspaceNotFoundException;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import com.amar.slackclone.workspace.dto.CreateWorkspaceRequest;
import com.amar.slackclone.workspace.dto.WorkspaceResponse;
import com.amar.slackclone.workspace.dto.WorkspaceMemberResponse;
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

public WorkspaceService(
        WorkspaceRepository workspaceRepository,
        WorkspaceMemberRepository workspaceMemberRepository,
        UserRepository userRepository
) {
    this.workspaceRepository = workspaceRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.userRepository = userRepository;
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
    User currentUser = userRepository
            .findByEmailIgnoreCase(authenticatedEmail)
            .orElseThrow(AuthenticatedUserNotFoundException::new);

    workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));

    workspaceMemberRepository
            .findByWorkspaceIdAndUserId(workspaceId, currentUser.getId())
            .orElseThrow(() -> new WorkspaceAccessDeniedException(
                    "You are not a member of this workspace"
            ));

    return workspaceMemberRepository
            .findAllByWorkspaceId(workspaceId)
            .stream()
            .map(this::toWorkspaceMemberResponse)
            .toList();
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
