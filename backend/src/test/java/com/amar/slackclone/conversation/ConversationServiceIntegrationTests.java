package com.amar.slackclone.conversation;

import com.amar.slackclone.conversation.dto.*;
import com.amar.slackclone.user.*;
import com.amar.slackclone.workspace.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ConversationServiceIntegrationTests {
    @Autowired ConversationService service;
    @Autowired UserRepository users;
    @Autowired WorkspaceRepository workspaces;
    @Autowired WorkspaceMemberRepository workspaceMembers;
    @Autowired ConversationParticipantRepository conversationParticipants;
    private User a, b, c, d, outsider;

    @BeforeEach
    void setUp() {
        String suffix = System.nanoTime() + "@example.com";
        a = saveUser("a" + suffix, "Amar"); b = saveUser("b" + suffix, "Faris");
        c = saveUser("c" + suffix, "Haris"); d = saveUser("d" + suffix, "Nedim"); outsider = saveUser("x" + suffix, "Outsider");
        Workspace workspace = workspaces.save(new Workspace("Team", "team-" + System.nanoTime(), a, Instant.now(), Instant.now()));
        workspaceMembers.save(new WorkspaceMember(workspace, a, WorkspaceRole.OWNER, Instant.now()));
        workspaceMembers.save(new WorkspaceMember(workspace, b, WorkspaceRole.MEMBER, Instant.now()));
        workspaceMembers.save(new WorkspaceMember(workspace, c, WorkspaceRole.MEMBER, Instant.now()));
        workspaceMembers.save(new WorkspaceMember(workspace, d, WorkspaceRole.MEMBER, Instant.now()));
        workspaceMembers.flush();
    }

    @Test
    void directConversationIsCanonicalInBothDirections() {
        var first = service.startDirect(new StartDirectConversationRequest(b.getId()), a.getEmail());
        var reverse = service.startDirect(new StartDirectConversationRequest(a.getId()), b.getEmail());
        assertEquals(first.id(), reverse.id());
        assertEquals(2, first.participants().size());
    }

    @Test
    void directValidationBlocksSelfAndUnrelatedUsers() {
        assertThrows(ConversationValidationException.class,
                () -> service.startDirect(new StartDirectConversationRequest(a.getId()), a.getEmail()));
        assertThrows(ConversationAccessDeniedException.class,
                () -> service.startDirect(new StartDirectConversationRequest(outsider.getId()), a.getEmail()));
    }

    @Test
    void groupIncludesCreatorDeduplicatesIdsAndRejectsOutsiders() {
        assertThrows(ConversationValidationException.class,
                () -> service.createGroup(new CreateGroupConversationRequest(List.of(b.getId())), a.getEmail()));
        var group = service.createGroup(new CreateGroupConversationRequest(List.of(b.getId(), b.getId(), c.getId())), a.getEmail());
        assertEquals(ConversationType.GROUP, group.type());
        assertEquals(3, group.participants().size());
        assertThrows(ConversationAccessDeniedException.class,
                () -> service.createGroup(new CreateGroupConversationRequest(List.of(b.getId(), outsider.getId())), a.getEmail()));
    }

    @Test
    void accessMessagesUnreadReadAndPaginationAreParticipantScoped() {
        var direct = service.startDirect(new StartDirectConversationRequest(b.getId()), a.getEmail());
        service.send(direct.id(), new CreateConversationMessageRequest("one"), a.getEmail());
        service.send(direct.id(), new CreateConversationMessageRequest("two"), a.getEmail());
        service.send(direct.id(), new CreateConversationMessageRequest("three"), a.getEmail());

        assertEquals(0, service.get(direct.id(), a.getEmail()).unreadCount());
        assertEquals(3, service.get(direct.id(), b.getEmail()).unreadCount());
        assertEquals(3, service.list(b.getEmail()).getFirst().unreadCount());
        assertThrows(ConversationAccessDeniedException.class, () -> service.get(direct.id(), outsider.getEmail()));
        assertThrows(ConversationAccessDeniedException.class, () -> service.history(direct.id(), null, 50, outsider.getEmail()));
        assertThrows(ConversationAccessDeniedException.class,
                () -> service.send(direct.id(), new CreateConversationMessageRequest("hack"), outsider.getEmail()));

        var newest = service.history(direct.id(), null, 2, b.getEmail());
        assertEquals(List.of("two", "three"), newest.messages().stream().map(ConversationMessageResponse::content).toList());
        assertNotNull(newest.nextBefore());
        var older = service.history(direct.id(), newest.nextBefore(), 2, b.getEmail());
        assertEquals(List.of("one"), older.messages().stream().map(ConversationMessageResponse::content).toList());

        assertEquals(0, service.markRead(direct.id(), b.getEmail()).unreadCount());
        assertEquals(0, service.markRead(direct.id(), b.getEmail()).unreadCount());
    }

    @Test
    void groupRenameCanBeSetAndClearedWhileDirectRenameAndOutsidersAreDenied() {
        var group = service.createGroup(new CreateGroupConversationRequest(List.of(b.getId(), c.getId())), a.getEmail());
        assertEquals("Backend Team", service.rename(group.id(), new UpdateConversationRequest(" Backend Team "), b.getEmail()).displayName());
        assertNull(service.rename(group.id(), new UpdateConversationRequest("  "), a.getEmail()).customName());
        assertThrows(ConversationAccessDeniedException.class,
                () -> service.rename(group.id(), new UpdateConversationRequest("Hack"), outsider.getEmail()));
        var direct = service.startDirect(new StartDirectConversationRequest(b.getId()), a.getEmail());
        assertThrows(ConversationValidationException.class,
                () -> service.rename(direct.id(), new UpdateConversationRequest("Wrong"), a.getEmail()));
    }

    @Test
    void senderCanEditAndSoftDeleteWithoutChangingUnreadCursorSemantics() {
        var direct = service.startDirect(new StartDirectConversationRequest(b.getId()), a.getEmail());
        var created = service.send(direct.id(), new CreateConversationMessageRequest("hello bro"), a.getEmail());
        var edited = service.editMessage(direct.id(), created.id(), new UpdateConversationMessageRequest("hello"), a.getEmail());
        assertEquals(created.createdAt(), edited.createdAt());
        assertTrue(edited.updatedAt().isAfter(edited.createdAt()) || edited.updatedAt().isEqual(edited.createdAt()));
        assertEquals(1, service.get(direct.id(), b.getEmail()).unreadCount());
        assertThrows(ConversationAccessDeniedException.class,
                () -> service.editMessage(direct.id(), created.id(), new UpdateConversationMessageRequest("hack"), b.getEmail()));
        assertThrows(ConversationAccessDeniedException.class,
                () -> service.editMessage(direct.id(), created.id(), new UpdateConversationMessageRequest("hack"), outsider.getEmail()));
        assertThrows(ConversationAccessDeniedException.class,
                () -> service.deleteMessage(direct.id(), created.id(), b.getEmail()));
        assertThrows(ConversationAccessDeniedException.class,
                () -> service.deleteMessage(direct.id(), created.id(), outsider.getEmail()));
        assertThrows(ConversationValidationException.class,
                () -> service.editMessage(direct.id(), created.id(), new UpdateConversationMessageRequest("  "), a.getEmail()));
        var deleted = service.deleteMessage(direct.id(), created.id(), a.getEmail());
        assertNull(deleted.content()); assertNotNull(deleted.deletedAt());
        assertEquals(0, service.get(direct.id(), b.getEmail()).unreadCount());
        assertNotNull(service.history(direct.id(), null, 50, b.getEmail()).messages().getFirst().deletedAt());

        var group = service.createGroup(new CreateGroupConversationRequest(List.of(b.getId(), c.getId())), a.getEmail());
        var groupMessage = service.send(group.id(), new CreateConversationMessageRequest("group draft"), a.getEmail());
        assertEquals("group final", service.editMessage(group.id(), groupMessage.id(),
                new UpdateConversationMessageRequest("group final"), a.getEmail()).content());
    }

    @Test
    void hidingIsPerParticipantAndStartingDirectOrReceivingMessageRestoresVisibility() {
        var direct = service.startDirect(new StartDirectConversationRequest(b.getId()), a.getEmail());
        service.hide(direct.id(), a.getEmail());
        assertTrue(service.list(a.getEmail()).isEmpty());
        assertEquals(direct.id(), service.list(b.getEmail()).getFirst().id());
        assertEquals(direct.id(), service.startDirect(new StartDirectConversationRequest(b.getId()), a.getEmail()).id());
        assertEquals(direct.id(), service.list(a.getEmail()).getFirst().id());
        service.hide(direct.id(), a.getEmail());
        service.send(direct.id(), new CreateConversationMessageRequest("welcome back"), b.getEmail());
        assertEquals(direct.id(), service.list(a.getEmail()).getFirst().id());
    }

    @Test
    void activeMembersCanListAndInviteWhileOutsidersAndFormerMembersCannot() {
        var group = service.createGroup(new CreateGroupConversationRequest(List.of(b.getId(), c.getId())), a.getEmail());
        assertEquals(3, service.participants(group.id(), b.getEmail()).size());
        assertEquals("CREATOR", service.participants(group.id(), b.getEmail()).stream()
                .filter(member -> member.userId().equals(a.getId())).findFirst().orElseThrow().role());
        assertThrows(ConversationAccessDeniedException.class, () -> service.participants(group.id(), outsider.getEmail()));
        assertThrows(ConversationAccessDeniedException.class, () -> service.addParticipants(group.id(),
                new AddConversationParticipantsRequest(List.of(outsider.getId())), b.getEmail()));
        service.addParticipants(group.id(), new AddConversationParticipantsRequest(List.of(d.getId())), b.getEmail());
        assertEquals(4, service.participants(group.id(), a.getEmail()).size());
        assertThrows(ConversationValidationException.class, () -> service.addParticipants(group.id(),
                new AddConversationParticipantsRequest(List.of(d.getId())), a.getEmail()));
        service.leave(group.id(), d.getEmail());
        assertThrows(ConversationAccessDeniedException.class, () -> service.participants(group.id(), d.getEmail()));
    }

    @Test
    void joiningAndRejoiningCreateNewPrivateHistoryBoundaries() {
        var group = service.createGroup(new CreateGroupConversationRequest(List.of(b.getId(), c.getId())), a.getEmail());
        service.send(group.id(), new CreateConversationMessageRequest("before join"), a.getEmail());
        service.addParticipants(group.id(), new AddConversationParticipantsRequest(List.of(d.getId())), b.getEmail());
        assertTrue(service.history(group.id(), null, 50, d.getEmail()).messages().isEmpty());
        assertEquals(0, service.get(group.id(), d.getEmail()).unreadCount());
        service.send(group.id(), new CreateConversationMessageRequest("after join"), a.getEmail());
        assertEquals(List.of("after join"), service.history(group.id(), null, 50, d.getEmail()).messages().stream().map(ConversationMessageResponse::content).toList());
        service.leave(group.id(), d.getEmail());
        assertThrows(ConversationAccessDeniedException.class, () -> service.send(group.id(), new CreateConversationMessageRequest("blocked"), d.getEmail()));
        service.send(group.id(), new CreateConversationMessageRequest("while away"), a.getEmail());
        var priorMembership = conversationParticipants.findByConversationIdAndUserId(group.id(), d.getId()).orElseThrow();
        var priorJoinedAt = priorMembership.getJoinedAt();
        service.addParticipants(group.id(), new AddConversationParticipantsRequest(List.of(d.getId())), a.getEmail());
        assertTrue(priorMembership.getJoinedAt().isAfter(priorJoinedAt));
        assertNull(priorMembership.getLeftAt());
        assertTrue(service.history(group.id(), null, 50, d.getEmail()).messages().isEmpty());
    }

    @Test
    void onlyCreatorRemovesOthersAndCreatorCannotLeaveWithMembers() {
        var group = service.createGroup(new CreateGroupConversationRequest(List.of(b.getId(), c.getId())), a.getEmail());
        assertThrows(ConversationAccessDeniedException.class, () -> service.removeParticipant(group.id(), c.getId(), b.getEmail()));
        assertThrows(ConversationValidationException.class, () -> service.removeParticipant(group.id(), a.getId(), a.getEmail()));
        service.removeParticipant(group.id(), c.getId(), a.getEmail());
        assertThrows(ConversationAccessDeniedException.class, () -> service.get(group.id(), c.getEmail()));
        assertTrue(service.list(c.getEmail()).isEmpty());
        assertThrows(ConversationValidationException.class, () -> service.leave(group.id(), a.getEmail()));
        service.leave(group.id(), b.getEmail());
        service.leave(group.id(), a.getEmail());
        assertTrue(service.list(a.getEmail()).isEmpty());
    }

    private User saveUser(String email, String name) {
        return users.save(new User(email, "hash", name, Instant.now(), Instant.now()));
    }
}
