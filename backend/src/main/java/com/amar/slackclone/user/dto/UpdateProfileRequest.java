package com.amar.slackclone.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 120) String title,
        @NotBlank @Size(min = 3, max = 32)
        @jakarta.validation.constraints.Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._]{2,31}$") String username
) {}
