package com.amar.slackclone.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @NotBlank(message = "Workspace name is required")
        @Size(min = 2, max = 100, message = "Workspace name must have between 2 and 100 characters")
        String name
) {}
