package com.amar.slackclone.message;

import com.amar.slackclone.channel.*;
import com.amar.slackclone.message.dto.CreateMessageRequest;
import com.amar.slackclone.notification.NotificationService;
import com.amar.slackclone.user.*;
import com.amar.slackclone.workspace.*;
import org.junit.jupiter.api.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MessageMentionResolutionTests {
    @Test void resolvesOnlyRealUsernameInAccessibleChannelMembers(){
        MessageRepository messages=mock(MessageRepository.class);ChannelAccessService access=mock(ChannelAccessService.class);UserRepository users=mock(UserRepository.class);
        ChannelMessageMentionRepository mentions=mock(ChannelMessageMentionRepository.class);WorkspaceMemberRepository workspaceMembers=mock(WorkspaceMemberRepository.class);
        NotificationService notifications=mock(NotificationService.class);User sender=user(1L,"sender","sender@example.com"),mentioned=user(2L,"amar.dev","amar@example.com");
        Workspace workspace=new Workspace("Team","team",sender,Instant.now(),Instant.now());ReflectionTestUtils.setField(workspace,"id",3L);
        Channel channel=new Channel();ReflectionTestUtils.setField(channel,"id",4L);channel.setWorkspace(workspace);channel.setName("general");
        when(access.validateChannelWriteAccess(3L,4L,sender.getEmail())).thenReturn(channel);when(users.findByEmailIgnoreCase(sender.getEmail())).thenReturn(Optional.of(sender));
        when(workspaceMembers.findAllByWorkspaceId(3L)).thenReturn(List.of(new WorkspaceMember(workspace,mentioned,WorkspaceRole.MEMBER,Instant.now())));
        when(messages.saveAndFlush(any())).thenAnswer(invocation->{Message m=invocation.getArgument(0);ReflectionTestUtils.setField(m,"id",8L);return m;});
        MessageService service=new MessageService(messages,access,users,mock(SimpMessagingTemplate.class),mock(ChannelPinnedMessageRepository.class),mentions,workspaceMembers,mock(ChannelMemberRepository.class),mock(ChannelMessageReactionRepository.class),mock(ChannelMessageAttachmentRepository.class),notifications,mock(com.amar.slackclone.messaging.producer.MessageEventProducer.class));
        TransactionSynchronizationManager.initSynchronization();
        try{service.createMessage(3L,4L,new CreateMessageRequest("Hello @amar.dev and @unknown"),sender.getEmail());}finally{TransactionSynchronizationManager.clearSynchronization();}
        var captor=org.mockito.ArgumentCaptor.forClass(ChannelMessageMention.class);verify(mentions,times(1)).save(captor.capture());assertEquals(mentioned.getId(),captor.getValue().getUser().getId());
        verify(notifications,times(1)).create(eq(mentioned.getId()),eq(sender.getId()),any(),any(),any());
    }
    private User user(Long id,String username,String email){User u=new User(email,"hash",username,Instant.now(),Instant.now());u.setUsername(username);ReflectionTestUtils.setField(u,"id",id);return u;}
}
