package com.amar.slackclone.message;

import com.amar.slackclone.channel.Channel;
import com.amar.slackclone.channel.ChannelAccessService;
import com.amar.slackclone.message.dto.UpdateMessageRequest;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageServiceMutationTests {
    private final MessageRepository repository = mock(MessageRepository.class);
    private final ChannelAccessService accessService = mock(ChannelAccessService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final com.amar.slackclone.messaging.producer.MessageEventProducer producer = mock(com.amar.slackclone.messaging.producer.MessageEventProducer.class);
    private MessageService service;
    private User sender;
    private Message message;

    @BeforeEach void setUp() {
        service = new MessageService(repository, accessService, userRepository, messaging,
                mock(ChannelPinnedMessageRepository.class), mock(ChannelMessageMentionRepository.class),
                mock(com.amar.slackclone.workspace.WorkspaceMemberRepository.class),
                mock(com.amar.slackclone.channel.ChannelMemberRepository.class),
                mock(ChannelMessageReactionRepository.class), mock(ChannelMessageAttachmentRepository.class),
                mock(com.amar.slackclone.notification.NotificationService.class), producer);
        sender = user(1L, "sender@example.com");
        Channel channel = new Channel(); ReflectionTestUtils.setField(channel, "id", 20L);
        message = new Message(); ReflectionTestUtils.setField(message, "id", 30L);
        ReflectionTestUtils.setField(message, "createdAt", OffsetDateTime.now().minusMinutes(2));
        ReflectionTestUtils.setField(message, "updatedAt", OffsetDateTime.now().minusMinutes(2));
        message.setChannel(channel); message.setSender(sender); message.setContent("secret original");
        when(userRepository.findByEmailIgnoreCase(sender.getEmail())).thenReturn(Optional.of(sender));
        when(repository.findByIdAndChannelId(30L, 20L)).thenReturn(Optional.of(message));
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach void tearDown() { TransactionSynchronizationManager.clearSynchronization(); }

    @Test void senderCanEditAndBroadcastIsDeferredUntilCommit() {
        var oldUpdatedAt = message.getUpdatedAt();
        var response = service.updateMessage(10L, 20L, 30L,
                new UpdateMessageRequest(" edited "), sender.getEmail());
        assertEquals("edited", response.content());
        assertTrue(message.getUpdatedAt().isAfter(oldUpdatedAt));
        verifyNoInteractions(messaging);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        verify(messaging).convertAndSend(eq("/topic/workspaces/10/channels/20/messages"), any(Object.class));
    }

    @Test void otherUserCannotEdit() {
        User other = user(2L, "other@example.com");
        when(userRepository.findByEmailIgnoreCase(other.getEmail())).thenReturn(Optional.of(other));
        assertThrows(MessageAccessDeniedException.class, () -> service.updateMessage(
                10L, 20L, 30L, new UpdateMessageRequest("changed"), other.getEmail()));
    }

    @Test void deletedMessageCannotBeEdited() {
        message.setDeletedAt(OffsetDateTime.now());
        assertThrows(MessageConflictException.class, () -> service.updateMessage(
                10L, 20L, 30L, new UpdateMessageRequest("changed"), sender.getEmail()));
    }

    @Test void senderSoftDeletesWithoutDeletingRowOrLeakingContent() {
        var response = service.deleteMessage(10L, 20L, 30L, sender.getEmail());
        assertNotNull(message.getDeletedAt()); assertNotNull(response.deletedAt());
        assertNull(response.content());
        verify(repository, never()).delete(any());
    }

    @Test void otherUserCannotDelete() {
        User other = user(2L, "other@example.com");
        when(userRepository.findByEmailIgnoreCase(other.getEmail())).thenReturn(Optional.of(other));
        assertThrows(MessageAccessDeniedException.class,
                () -> service.deleteMessage(10L, 20L, 30L, other.getEmail()));
    }

    @Test void messageCannotBeDeletedTwice() {
        message.setDeletedAt(OffsetDateTime.now());
        assertThrows(MessageConflictException.class,
                () -> service.deleteMessage(10L, 20L, 30L, sender.getEmail()));
    }

    @Test void historyIncludesDeletedPositionWithoutContent() {
        message.setDeletedAt(OffsetDateTime.now());
        when(repository.findAllByChannelIdOrderByCreatedAtAsc(20L)).thenReturn(List.of(message));
        var history = service.getMessages(10L, 20L, sender.getEmail());
        assertEquals(1, history.size()); assertNull(history.getFirst().content());
        assertNotNull(history.getFirst().deletedAt());
    }

    @Test void creationPublishesSavedSnapshotOnlyAfterCommit() {
        when(accessService.validateChannelWriteAccess(10L, 20L, sender.getEmail())).thenReturn(message.getChannel());
        when(repository.saveAndFlush(any())).thenReturn(message);
        var response = service.createMessage(10L, 20L,
                new com.amar.slackclone.message.dto.CreateMessageRequest("hello"), sender.getEmail());
        verifyNoInteractions(producer);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        verify(producer).send(new com.amar.slackclone.messaging.event.MessageCreatedEvent(10L, response, null));
        verifyNoInteractions(messaging);
    }

    @Test void replyPublishesReplyAndUpdatedRootInOneEvent() {
        when(accessService.validateChannelWriteAccess(10L, 20L, sender.getEmail())).thenReturn(message.getChannel());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 31L);
            return saved;
        });
        when(repository.countByThreadRootMessageId(30L)).thenReturn(1L);
        var response = service.reply(10L, 20L, 30L,
                new com.amar.slackclone.message.dto.CreateMessageRequest("reply"), sender.getEmail());
        verifyNoInteractions(producer, messaging);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        var captor = org.mockito.ArgumentCaptor.forClass(com.amar.slackclone.messaging.event.MessageCreatedEvent.class);
        verify(producer).send(captor.capture());
        assertEquals(response, captor.getValue().message());
        assertEquals(30L, response.threadRootMessageId());
        assertEquals(30L, captor.getValue().threadRoot().id());
        assertEquals(1L, captor.getValue().threadRoot().replyCount());
        verifyNoInteractions(messaging);
    }

    @Test void failedSaveDoesNotPublish() {
        when(accessService.validateChannelWriteAccess(10L, 20L, sender.getEmail())).thenReturn(message.getChannel());
        when(repository.saveAndFlush(any())).thenThrow(new IllegalStateException("save failed"));
        assertThrows(IllegalStateException.class, () -> service.createMessage(10L, 20L,
                new com.amar.slackclone.message.dto.CreateMessageRequest("hello"), sender.getEmail()));
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        verifyNoInteractions(producer, messaging);
    }

    @Test void rollbackDoesNotPublish() {
        when(accessService.validateChannelWriteAccess(10L, 20L, sender.getEmail())).thenReturn(message.getChannel());
        when(repository.saveAndFlush(any())).thenReturn(message);
        service.createMessage(10L, 20L, new com.amar.slackclone.message.dto.CreateMessageRequest("hello"), sender.getEmail());
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(
                org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(producer, messaging);
    }

    private User user(Long id, String email) {
        User user = new User(email, "hash", email, Instant.now(), Instant.now());
        ReflectionTestUtils.setField(user, "id", id); return user;
    }
}
