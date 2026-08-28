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
    private User a, b, c, outsider;

    @BeforeEach
    void setUp() {
        String suffix = System.nanoTime() + "@example.com";
        a = saveUser("a" + suffix, "Amar"); b = saveUser("b" + suffix, "Faris");
        c = saveUser("c" + suffix, "Haris"); outsider = saveUser("d" + suffix, "Outsider");
        Workspace workspace = workspaces.save(new Workspace("Team", "team-" + System.nanoTime(), a, Instant.now(), Instant.now()));
        workspaceMembers.save(new WorkspaceMember(workspace, a, WorkspaceRole.OWNER, Instant.now()));
        workspaceMembers.save(new WorkspaceMember(workspace, b, WorkspaceRole.MEMBER, Instant.now()));
        workspaceMembers.save(new WorkspaceMember(workspace, c, WorkspaceRole.MEMBER, Instant.now()));
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

    private User saveUser(String email, String name) {
        return users.save(new User(email, "hash", name, Instant.now(), Instant.now()));
    }
}
