package com.amar.slackclone.conversation;

import com.amar.slackclone.conversation.dto.CreateConversationMessageRequest;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class ConversationWebSocketController {
    private final ConversationService service;
    public ConversationWebSocketController(ConversationService service) { this.service = service; }
    @MessageMapping("/conversations/{conversationId}/messages")
    public void send(@DestinationVariable Long conversationId, @Valid @Payload CreateConversationMessageRequest request,
            Authentication authentication) {
        service.send(conversationId, request, authentication.getName());
    }
}
