package com.amar.slackclone.message;

import com.amar.slackclone.message.dto.CreateMessageRequest;
import com.amar.slackclone.message.dto.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/channels/{channelId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(
        @PathVariable Long workspaceId,
        @PathVariable Long channelId,
        Authentication authentication
    ) {
        List<MessageResponse> messages = messageService.getMessages(
            workspaceId,
            channelId,
            authentication.getName()
        );

        return ResponseEntity.ok(messages);
    }

    @PostMapping
    public ResponseEntity<MessageResponse> createMessage(
        @PathVariable Long workspaceId,
        @PathVariable Long channelId,
        @Valid @RequestBody CreateMessageRequest request,
        Authentication authentication
    ) {
        MessageResponse message = messageService.createMessage(
            workspaceId,
            channelId,
            request,
            authentication.getName()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(message);
    }
}