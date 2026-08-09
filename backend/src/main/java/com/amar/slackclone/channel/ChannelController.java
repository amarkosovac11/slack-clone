package com.amar.slackclone.channel;

import com.amar.slackclone.channel.dto.ChannelResponse;
import com.amar.slackclone.channel.dto.CreateChannelRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping
    public ResponseEntity<List<ChannelResponse>> getChannels(
            @PathVariable Long workspaceId,
            Authentication authentication
    ) {
        String currentUserEmail = authentication.getName();

        List<ChannelResponse> channels =
                channelService.getChannels(workspaceId, currentUserEmail);

        return ResponseEntity.ok(channels);
    }

    @PostMapping
    public ResponseEntity<ChannelResponse> createChannel(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateChannelRequest request,
            Authentication authentication
    ) {
        String currentUserEmail = authentication.getName();

        ChannelResponse channel =
                channelService.createChannel(
                        workspaceId,
                        request,
                        currentUserEmail
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(channel);
    }
}