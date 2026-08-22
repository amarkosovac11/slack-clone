package com.amar.slackclone.workspace;

import com.amar.slackclone.workspace.dto.WorkspaceInvitationResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService workspaceInvitationService;

    public WorkspaceInvitationController(
            WorkspaceInvitationService workspaceInvitationService
    ) {
        this.workspaceInvitationService = workspaceInvitationService;
    }

    @GetMapping
    public List<WorkspaceInvitationResponse> getCurrentUserPendingInvitations(
            Authentication authentication
    ) {
        return workspaceInvitationService.getCurrentUserPendingInvitations(
                authentication.getName()
        );
    }

    @PostMapping("/{invitationId}/accept")
    public WorkspaceInvitationResponse acceptInvitation(
            @PathVariable Long invitationId,
            Authentication authentication
    ) {
        return workspaceInvitationService.acceptInvitation(
                invitationId,
                authentication.getName()
        );
    }

    @PostMapping("/{invitationId}/reject")
    public WorkspaceInvitationResponse rejectInvitation(
            @PathVariable Long invitationId,
            Authentication authentication
    ) {
        return workspaceInvitationService.rejectInvitation(
                invitationId,
                authentication.getName()
        );
    }
}
