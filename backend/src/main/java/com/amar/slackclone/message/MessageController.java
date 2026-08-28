package com.amar.slackclone.message;

import com.amar.slackclone.message.dto.CreateMessageRequest;
import com.amar.slackclone.message.dto.MessageResponse;
import com.amar.slackclone.message.dto.UpdateMessageRequest;
import com.amar.slackclone.message.dto.PinnedMessageResponse;
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

    @PatchMapping("/{messageId}")
    public MessageResponse updateMessage(@PathVariable Long workspaceId, @PathVariable Long channelId,
            @PathVariable Long messageId, @Valid @RequestBody UpdateMessageRequest request,
            Authentication authentication) {
        return messageService.updateMessage(workspaceId, channelId, messageId, request, authentication.getName());
    }

    @DeleteMapping("/{messageId}")
    public MessageResponse deleteMessage(@PathVariable Long workspaceId, @PathVariable Long channelId,
            @PathVariable Long messageId, Authentication authentication) {
        return messageService.deleteMessage(workspaceId, channelId, messageId, authentication.getName());
    }
    @PostMapping("/{messageId}/pin") public PinnedMessageResponse pin(@PathVariable Long workspaceId,@PathVariable Long channelId,@PathVariable Long messageId,Authentication a){return messageService.pin(workspaceId,channelId,messageId,a.getName());}
    @DeleteMapping("/{messageId}/pin") @ResponseStatus(HttpStatus.NO_CONTENT) public void unpin(@PathVariable Long workspaceId,@PathVariable Long channelId,@PathVariable Long messageId,Authentication a){messageService.unpin(workspaceId,channelId,messageId,a.getName());}
    @GetMapping("/pins") public List<PinnedMessageResponse> pins(@PathVariable Long workspaceId,@PathVariable Long channelId,Authentication a){return messageService.pins(workspaceId,channelId,a.getName());}
}
