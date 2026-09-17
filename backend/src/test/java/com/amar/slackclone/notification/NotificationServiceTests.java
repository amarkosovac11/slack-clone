package com.amar.slackclone.notification;

import com.amar.slackclone.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTests {
    @Test void createsForRecipientButNeverForActorAndEnforcesOwnership(){
        NotificationRepository repository=mock(NotificationRepository.class);UserRepository users=mock(UserRepository.class);
        NotificationService service=new NotificationService(repository,users,mock(SimpMessagingTemplate.class));
        User recipient=user(1L,"recipient@example.com"),actor=user(2L,"actor@example.com");
        when(users.findById(1L)).thenReturn(Optional.of(recipient));when(users.findById(2L)).thenReturn(Optional.of(actor));
        when(repository.saveAndFlush(any())).thenAnswer(invocation->{Notification n=invocation.getArgument(0);ReflectionTestUtils.setField(n,"id",9L);ReflectionTestUtils.setField(n,"createdAt",java.time.OffsetDateTime.now());return n;});
        assertNotNull(service.create(1L,2L,NotificationType.MENTION,"Actor mentioned you",null));
        assertNull(service.create(2L,2L,NotificationType.MENTION,"self",null));verify(repository,times(1)).saveAndFlush(any());
        when(users.findByEmailIgnoreCase("recipient@example.com")).thenReturn(Optional.of(recipient));when(repository.findByIdAndRecipientId(9L,1L)).thenReturn(Optional.empty());
        assertThrows(SecurityException.class,()->service.markRead(9L,"recipient@example.com"));
    }
    private User user(Long id,String email){User u=new User(email,"hash",email,Instant.now(),Instant.now());u.setUsername("user_"+id);ReflectionTestUtils.setField(u,"id",id);return u;}
}
