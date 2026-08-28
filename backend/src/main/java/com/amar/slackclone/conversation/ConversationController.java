package com.amar.slackclone.conversation;

import com.amar.slackclone.conversation.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @Validated @RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService service;
    public ConversationController(ConversationService service) { this.service = service; }
    @GetMapping public List<ConversationResponse> list(Authentication auth) { return service.list(auth.getName()); }
    @GetMapping("/hidden") public List<ConversationResponse> hidden(Authentication auth){return service.hidden(auth.getName());}
    @GetMapping("/eligible-users") public List<ConversationUserResponse> eligible(Authentication auth) { return service.eligibleUsers(auth.getName()); }
    @PostMapping("/direct")
    public ConversationResponse direct(@Valid @RequestBody StartDirectConversationRequest request, Authentication auth) {
        return service.startDirect(request, auth.getName());
    }
    @PostMapping("/group") @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse group(@Valid @RequestBody CreateGroupConversationRequest request, Authentication auth) {
        return service.createGroup(request, auth.getName());
    }
    @GetMapping("/{id}") public ConversationResponse get(@PathVariable Long id, Authentication auth) { return service.get(id, auth.getName()); }
    @GetMapping("/{id}/participants") public List<ConversationParticipantResponse> participants(@PathVariable Long id, Authentication auth) {
        return service.participants(id, auth.getName());
    }
    @GetMapping("/{id}/eligible-users") public List<ConversationUserResponse> eligibleParticipants(@PathVariable Long id, Authentication auth) {
        return service.eligibleParticipants(id, auth.getName());
    }
    @PostMapping("/{id}/participants") public ConversationResponse addParticipants(@PathVariable Long id,
            @Valid @RequestBody AddConversationParticipantsRequest request, Authentication auth) {
        return service.addParticipants(id, request, auth.getName());
    }
    @DeleteMapping("/{id}/participants/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeParticipant(@PathVariable Long id, @PathVariable Long userId, Authentication auth) {
        service.removeParticipant(id, userId, auth.getName());
    }
    @PostMapping("/{id}/leave") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long id, Authentication auth) { service.leave(id, auth.getName()); }
    @GetMapping("/{id}/messages")
    public ConversationMessagePageResponse history(@PathVariable Long id, @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit, Authentication auth) {
        return service.history(id, before, limit, auth.getName());
    }
    @PostMapping("/{id}/messages") @ResponseStatus(HttpStatus.CREATED)
    public ConversationMessageResponse send(@PathVariable Long id, @Valid @RequestBody CreateConversationMessageRequest request, Authentication auth) {
        return service.send(id, request, auth.getName());
    }
    @PostMapping("/{id}/read") public ConversationResponse read(@PathVariable Long id, Authentication auth) {
        return service.markRead(id, auth.getName());
    }
    @PatchMapping("/{id}") public ConversationResponse rename(@PathVariable Long id,
            @Valid @RequestBody UpdateConversationRequest request, Authentication auth) {
        return service.rename(id, request, auth.getName());
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hide(@PathVariable Long id, Authentication auth) { service.hide(id, auth.getName()); }
    @PostMapping("/{id}/restore") public ConversationResponse restore(@PathVariable Long id,Authentication auth){return service.restore(id,auth.getName());}
    @PostMapping("/{id}/transfer-creator") public ConversationResponse transferCreator(@PathVariable Long id,@Valid @RequestBody TransferConversationCreatorRequest request,Authentication auth){return service.transferCreator(id,request,auth.getName());}
    @GetMapping("/{id}/messages/{messageId}/receipt") public ConversationReadReceiptResponse receipt(@PathVariable Long id,@PathVariable Long messageId,Authentication auth){return service.receipt(id,messageId,auth.getName());}
    @PatchMapping("/{id}/messages/{messageId}")
    public ConversationMessageResponse editMessage(@PathVariable Long id, @PathVariable Long messageId,
            @Valid @RequestBody UpdateConversationMessageRequest request, Authentication auth) {
        return service.editMessage(id, messageId, request, auth.getName());
    }
    @DeleteMapping("/{id}/messages/{messageId}")
    public ConversationMessageResponse deleteMessage(@PathVariable Long id, @PathVariable Long messageId, Authentication auth) {
        return service.deleteMessage(id, messageId, auth.getName());
    }
}
