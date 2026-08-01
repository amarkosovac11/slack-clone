package com.amar.slackclone.workspace;

import com.amar.slackclone.auth.InvalidCredentialsException;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import com.amar.slackclone.workspace.dto.CreateWorkspaceRequest;
import com.amar.slackclone.workspace.dto.WorkspaceResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
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

        workspaceMemberRepository.save(ownerMembership);

        return new WorkspaceResponse(
                savedWorkspace.getId(),
                savedWorkspace.getName(),
                savedWorkspace.getSlug(),
                currentUser.getId(),
                WorkspaceRole.OWNER,
                savedWorkspace.getCreatedAt(),
                savedWorkspace.getUpdatedAt()
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