package com.amar.slackclone.config;

import com.amar.slackclone.channel.ChannelAccessService;
import com.amar.slackclone.conversation.ConversationAccessService;
import com.amar.slackclone.security.JwtService;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private static final Pattern CHANNEL_TOPIC = Pattern.compile(
        "^/topic/workspaces/(\\d+)/channels/(\\d+)/(?:messages|typing)$"
    );
    private static final Pattern CONVERSATION_TOPIC = Pattern.compile("^/topic/users/(\\d+)/conversations/(\\d+)/(?:messages|metadata|typing)$");
    private static final Pattern USER_CONVERSATION_TOPIC = Pattern.compile("^/topic/users/(\\d+)/conversations$");
    private static final Pattern CONVERSATION_SEND = Pattern.compile("^/app/conversations/(\\d+)/messages$");
    private static final Pattern CONVERSATION_TYPING_SEND=Pattern.compile("^/app/conversations/(\\d+)/typing$");
    private static final Pattern CHANNEL_TYPING_SEND=Pattern.compile("^/app/workspaces/(\\d+)/channels/(\\d+)/typing$");

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ChannelAccessService channelAccessService;
    private final ConversationAccessService conversationAccessService;

    public WebSocketSecurityInterceptor(
        JwtService jwtService,
        UserRepository userRepository,
        ChannelAccessService channelAccessService,
        ConversationAccessService conversationAccessService
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.channelAccessService = channelAccessService;
        this.conversationAccessService = conversationAccessService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            message,
            StompHeaderAccessor.class
        );

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            authorizeSend(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing WebSocket bearer token");
        }

        String token = authorization.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid WebSocket bearer token");
        }

        User user = userRepository
            .findByEmailIgnoreCase(jwtService.extractEmail(token))
            .orElseThrow(() -> new IllegalArgumentException("Unknown WebSocket user"));

        accessor.setUser(new UsernamePasswordAuthenticationToken(
            user.getEmail(),
            null,
            Collections.emptyList()
        ));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new IllegalArgumentException("Unauthenticated WebSocket subscription");
        }

        String destination = accessor.getDestination();
        String safeDestination = destination == null ? "" : destination;
        Matcher matcher = CHANNEL_TOPIC.matcher(safeDestination);
        if (matcher.matches()) {
            channelAccessService.validateChannelAccess(Long.valueOf(matcher.group(1)), Long.valueOf(matcher.group(2)), accessor.getUser().getName());
            return;
        }
        matcher = CONVERSATION_TOPIC.matcher(safeDestination);
        if (matcher.matches()) {
            var user = conversationAccessService.requireUser(accessor.getUser().getName());
            if (!user.getId().equals(Long.valueOf(matcher.group(1)))) throw new IllegalArgumentException("Cannot subscribe to another user's conversation");
            conversationAccessService.requireParticipant(Long.valueOf(matcher.group(2)), accessor.getUser().getName()); return;
        }
        matcher = USER_CONVERSATION_TOPIC.matcher(safeDestination);
        if (matcher.matches()) {
            var user = conversationAccessService.requireUser(accessor.getUser().getName());
            if (!user.getId().equals(Long.valueOf(matcher.group(1)))) throw new IllegalArgumentException("Cannot subscribe to another user's updates");
            return;
        }
        throw new IllegalArgumentException("WebSocket subscription destination is not allowed");
    }

    private void authorizeSend(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) throw new IllegalArgumentException("Unauthenticated WebSocket send");
        String destination=accessor.getDestination()==null?"":accessor.getDestination();Matcher matcher=CONVERSATION_SEND.matcher(destination);
        if(matcher.matches()){conversationAccessService.requireParticipant(Long.valueOf(matcher.group(1)),accessor.getUser().getName());return;}matcher=CONVERSATION_TYPING_SEND.matcher(destination);if(matcher.matches()){conversationAccessService.requireParticipant(Long.valueOf(matcher.group(1)),accessor.getUser().getName());return;}matcher=CHANNEL_TYPING_SEND.matcher(destination);if(matcher.matches()){channelAccessService.validateChannelAccess(Long.valueOf(matcher.group(1)),Long.valueOf(matcher.group(2)),accessor.getUser().getName());return;}throw new IllegalArgumentException("WebSocket send destination is not allowed");
    }
}
