package com.amar.slackclone.message.dto; import jakarta.validation.constraints.*;
public record ReactionRequest(@NotBlank @Size(max=16) String emoji){}
