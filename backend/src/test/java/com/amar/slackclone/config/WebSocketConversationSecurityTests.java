package com.amar.slackclone.config;

import com.amar.slackclone.channel.ChannelAccessService;
import com.amar.slackclone.conversation.*;
import com.amar.slackclone.security.JwtService;
import com.amar.slackclone.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class WebSocketConversationSecurityTests {
    private final ConversationAccessService access = mock(ConversationAccessService.class);
    private final WebSocketSecurityInterceptor interceptor = new WebSocketSecurityInterceptor(
            mock(JwtService.class), mock(UserRepository.class), mock(ChannelAccessService.class), access);

    @Test
    void participantSubscriptionAndSendAreCheckedServerSide() {
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/conversations/12/messages", "user@example.com"), null);
        interceptor.preSend(frame(StompCommand.SEND, "/app/conversations/12/messages", "user@example.com"), null);
        verify(access, times(2)).requireParticipant(12L, "user@example.com");
    }

    @Test
    void outsiderCannotSubscribeOrSend() {
        when(access.requireParticipant(12L, "outsider@example.com"))
                .thenThrow(new ConversationAccessDeniedException("denied"));
        assertThrows(ConversationAccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/conversations/12/messages", "outsider@example.com"), null));
        assertThrows(ConversationAccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SEND, "/app/conversations/12/messages", "outsider@example.com"), null));
    }

    @Test
    void userCannotSubscribeToAnotherUsersSidebarUpdates() {
        User user = new User("user@example.com", "hash", "User", Instant.now(), Instant.now());
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 7L);
        when(access.requireUser("user@example.com")).thenReturn(user);
        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/users/8/conversations", "user@example.com"), null));
    }

    private org.springframework.messaging.Message<byte[]> frame(StompCommand command, String destination, String email) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(email, null));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
