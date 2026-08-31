package com.amar.slackclone.presence;

import com.amar.slackclone.user.*;
import com.amar.slackclone.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PresenceServiceTests {
    private final UserRepository users=mock(UserRepository.class);
    private final WorkspaceMemberRepository members=mock(WorkspaceMemberRepository.class);
    private final SimpMessagingTemplate messaging=mock(SimpMessagingTemplate.class);
    private final PresenceService service=new PresenceService(users,members,messaging);
    private final User user=user();

    @Test void multipleSessionsOnlyGoOfflineAfterFinalDisconnect(){
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));when(users.findById(1L)).thenReturn(Optional.of(user));when(members.findMessageableUsers(1L)).thenReturn(List.of());
        service.connect("one",user.getEmail());service.connect("two",user.getEmail());
        assertEquals(2,service.sessionCount(1L));assertEquals(UserPresenceStatus.ONLINE,service.status(1L));
        service.disconnect("one");assertEquals(UserPresenceStatus.ONLINE,service.status(1L));assertNull(user.getLastSeenAt());
        service.disconnect("two");assertEquals(UserPresenceStatus.OFFLINE,service.status(1L));assertNotNull(user.getLastSeenAt());
    }

    @Test void activityCannotSpoofAnotherUser(){
        User other=new User("other@example.com","hash","Other",Instant.now(),Instant.now());ReflectionTestUtils.setField(other,"id",2L);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));when(users.findByEmailIgnoreCase(other.getEmail())).thenReturn(Optional.of(other));when(members.findMessageableUsers(1L)).thenReturn(List.of());
        service.connect("one",user.getEmail());service.activity("one",other.getEmail());assertEquals(1,service.sessionCount(1L));assertEquals(0,service.sessionCount(2L));
    }
    private User user(){User u=new User("user@example.com","hash","User",Instant.now(),Instant.now());ReflectionTestUtils.setField(u,"id",1L);return u;}
}
