package com.amar.slackclone.workspace;

import com.amar.slackclone.channel.AuthenticatedUserNotFoundException;
import com.amar.slackclone.channel.WorkspaceAccessDeniedException;
import com.amar.slackclone.channel.WorkspaceNotFoundException;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceAccessService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public WorkspaceAccessService(WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository, UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireWorkspaceMember(Long workspaceId, String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(AuthenticatedUserNotFoundException::new);
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() -> new WorkspaceAccessDeniedException("You are not a member of this workspace"));
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireOwner(Long workspaceId, String email) {
        WorkspaceMember membership = requireWorkspaceMember(workspaceId, email);
        if (!isOwner(membership)) {
            throw new WorkspaceAccessDeniedException("Only the workspace owner can perform this operation");
        }
        return membership;
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireOwnerOrAdmin(Long workspaceId, String email) {
        WorkspaceMember membership = requireWorkspaceMember(workspaceId, email);
        if (!isOwnerOrAdmin(membership)) {
            throw new WorkspaceAccessDeniedException("Only workspace owners and admins can perform this operation");
        }
        return membership;
    }

    public boolean isOwner(WorkspaceMember membership) {
        return membership.getRole() == WorkspaceRole.OWNER;
    }

    public boolean isAdmin(WorkspaceMember membership) {
        return membership.getRole() == WorkspaceRole.ADMIN;
    }

    public boolean isOwnerOrAdmin(WorkspaceMember membership) {
        return isOwner(membership) || isAdmin(membership);
    }
}
