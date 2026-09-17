package com.amar.slackclone.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
        String password,

        @NotBlank(message = "Display name is required")
        @Size(min = 2, max = 100, message = "Display name must contain between 2 and 100 characters")
        String displayName,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 32, message = "Username must contain between 3 and 32 characters")
        @jakarta.validation.constraints.Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._]{2,31}$",
                message = "Username may contain letters, numbers, dots and underscores")
        String username

) {
}
