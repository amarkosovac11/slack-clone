package com.amar.slackclone.conversation;

import com.amar.slackclone.conversation.dto.CreateConversationMessageRequest;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class ConversationWebSocketController {
    private final ConversationService service;
    private final ConversationReceiptService receipts;
    public ConversationWebSocketController(ConversationService service,ConversationReceiptService receipts) { this.service = service;this.receipts=receipts; }
    @MessageMapping("/conversations/{conversationId}/messages")
    public void send(@DestinationVariable Long conversationId, @Valid @Payload CreateConversationMessageRequest request,
            Authentication authentication) {
        service.send(conversationId, request, authentication.getName());
    }
    @MessageMapping("/conversations/{conversationId}/messages/{messageId}/delivered")
    public void delivered(@DestinationVariable Long conversationId,@DestinationVariable Long messageId,Authentication authentication){
        receipts.acknowledgeDelivered(conversationId,messageId,authentication.getName());
    }
}
