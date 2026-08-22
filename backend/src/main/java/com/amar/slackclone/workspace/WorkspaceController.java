package com.amar.slackclone.workspace;

import com.amar.slackclone.workspace.dto.CreateWorkspaceRequest;
import com.amar.slackclone.workspace.dto.CreateWorkspaceInvitationRequest;
import com.amar.slackclone.workspace.dto.WorkspaceInvitationResponse;
import com.amar.slackclone.workspace.dto.WorkspaceResponse;
import com.amar.slackclone.workspace.dto.WorkspaceMemberResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final WorkspaceInvitationService workspaceInvitationService;

    public WorkspaceController(
            WorkspaceService workspaceService,
            WorkspaceInvitationService workspaceInvitationService
    ) {
        this.workspaceService = workspaceService;
        this.workspaceInvitationService = workspaceInvitationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request,
            Authentication authentication
    ) {
        return workspaceService.createWorkspace(
                request,
                authentication.getName()
        );
    }

    @PostMapping("/{workspaceId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceInvitationResponse createInvitation(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateWorkspaceInvitationRequest request,
            Authentication authentication
    ) {
        return workspaceInvitationService.createInvitation(
                workspaceId,
                request,
                authentication.getName()
        );
    }

    @GetMapping
    public List<WorkspaceResponse> getCurrentUserWorkspaces(
            Authentication authentication
    ) {
        return workspaceService.getCurrentUserWorkspaces(
                authentication.getName()
        );
    }

    @GetMapping("/{workspaceId}/members")
    public List<WorkspaceMemberResponse> getWorkspaceMembers(
            @PathVariable Long workspaceId,
            Authentication authentication
    ) {
        return workspaceService.getWorkspaceMembers(
                workspaceId,
                authentication.getName()
        );
    }
}
