package com.amar.slackclone.channel;

import com.amar.slackclone.user.*;
import com.amar.slackclone.workspace.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChannelAccessServiceArchiveTests {
    @Test void archivedChannelCanBeReadButCannotBeWritten() {
        ChannelRepository channels=mock(ChannelRepository.class);ChannelMemberRepository channelMembers=mock(ChannelMemberRepository.class);
        WorkspaceRepository workspaces=mock(WorkspaceRepository.class);WorkspaceMemberRepository members=mock(WorkspaceMemberRepository.class);UserRepository users=mock(UserRepository.class);
        User user=new User("reader@example.com","hash","Reader",Instant.now(),Instant.now());ReflectionTestUtils.setField(user,"id",1L);
        Channel channel=new Channel();ReflectionTestUtils.setField(channel,"id",2L);channel.setArchivedAt(OffsetDateTime.now());
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));when(workspaces.existsById(3L)).thenReturn(true);
        when(members.existsByWorkspaceIdAndUserId(3L,1L)).thenReturn(true);when(channels.findByIdAndWorkspaceId(2L,3L)).thenReturn(Optional.of(channel));
        ChannelAccessService service=new ChannelAccessService(channels,channelMembers,workspaces,members,users);
        assertSame(channel,service.validateChannelAccess(3L,2L,user.getEmail()));
        ChannelConflictException error=assertThrows(ChannelConflictException.class,()->service.validateChannelWriteAccess(3L,2L,user.getEmail()));
        assertEquals("Archived channels are read-only",error.getMessage());
    }
}
