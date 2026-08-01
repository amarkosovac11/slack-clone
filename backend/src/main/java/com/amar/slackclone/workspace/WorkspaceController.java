package com.amar.slackclone.workspace;

import com.amar.slackclone.workspace.dto.CreateWorkspaceRequest;
import com.amar.slackclone.workspace.dto.WorkspaceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(
            WorkspaceService workspaceService
    ) {
        this.workspaceService = workspaceService;
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
}
