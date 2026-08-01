package com.amar.slackclone.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(

        @NotBlank(message = "Workspace name is required")
        @Size(
                min = 2,
                max = 100,
                message = "Workspace name must contain between 2 and 100 characters"
        )
        String name

) {
}