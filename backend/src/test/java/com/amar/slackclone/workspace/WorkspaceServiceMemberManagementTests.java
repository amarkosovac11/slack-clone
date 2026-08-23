package com.amar.slackclone.workspace;

import com.amar.slackclone.channel.ChannelMemberRepository;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import com.amar.slackclone.workspace.dto.UpdateWorkspaceMemberRoleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceServiceMemberManagementTests {
    private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
    private final WorkspaceMemberRepository memberRepository = mock(WorkspaceMemberRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ChannelMemberRepository channelMemberRepository = mock(ChannelMemberRepository.class);
    private WorkspaceService service;
    private Workspace workspace;
    private User actor;

    @BeforeEach
    void setUp() {
        service = new WorkspaceService(
                workspaceRepository,
                memberRepository,
                userRepository,
                channelMemberRepository
        );
        actor = user(1L, "actor@example.com", "Actor");
        workspace = new Workspace("Team", "team", actor, Instant.now(), Instant.now());
        ReflectionTestUtils.setField(workspace, "id", 10L);
        when(userRepository.findByEmailIgnoreCase(actor.getEmail())).thenReturn(Optional.of(actor));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
    }

    @ParameterizedTest
    @EnumSource(value = WorkspaceRole.class, names = {"ADMIN", "MEMBER"})
    void ownerCanAssignManageableRoles(WorkspaceRole role) {
        WorkspaceMember owner = membership(actor, WorkspaceRole.OWNER);
        WorkspaceMember target = membership(user(2L, "target@example.com", "Target"), WorkspaceRole.MEMBER);
        stubMemberships(owner, target);

        var response = service.updateWorkspaceMemberRole(
                10L, 2L, new UpdateWorkspaceMemberRoleRequest(role), actor.getEmail()
        );

        assertEquals(role, response.role());
        assertEquals(role, target.getRole());
    }

    @Test
    void ownerCannotAssignOwner() {
        stubMemberships(
                membership(actor, WorkspaceRole.OWNER),
                membership(user(2L, "target@example.com", "Target"), WorkspaceRole.MEMBER)
        );
        assertThrows(WorkspaceMemberConflictException.class, () -> service.updateWorkspaceMemberRole(
                10L, 2L, new UpdateWorkspaceMemberRoleRequest(WorkspaceRole.OWNER), actor.getEmail()
        ));
    }

    @Test
    void ownerMembershipCannotBeModified() {
        WorkspaceMember owner = membership(actor, WorkspaceRole.OWNER);
        stubMemberships(owner, owner);
        assertThrows(WorkspaceMemberConflictException.class, () -> service.updateWorkspaceMemberRole(
                10L, 1L, new UpdateWorkspaceMemberRoleRequest(WorkspaceRole.MEMBER), actor.getEmail()
        ));
    }

    @ParameterizedTest
    @EnumSource(value = WorkspaceRole.class, names = {"ADMIN", "MEMBER"})
    void nonOwnerCannotChangeRoles(WorkspaceRole actorRole) {
        stubMemberships(
                membership(actor, actorRole),
                membership(user(2L, "target@example.com", "Target"), WorkspaceRole.MEMBER)
        );
        assertThrows(WorkspaceMemberAccessDeniedException.class, () -> service.updateWorkspaceMemberRole(
                10L, 2L, new UpdateWorkspaceMemberRoleRequest(WorkspaceRole.ADMIN), actor.getEmail()
        ));
    }

    @ParameterizedTest
    @EnumSource(value = WorkspaceRole.class, names = {"ADMIN", "MEMBER"})
    void ownerCanRemoveAdminOrMember(WorkspaceRole targetRole) {
        WorkspaceMember target = membership(user(2L, "target@example.com", "Target"), targetRole);
        stubMemberships(membership(actor, WorkspaceRole.OWNER), target);

        service.removeWorkspaceMember(10L, 2L, actor.getEmail());

        verify(channelMemberRepository).deleteAllByWorkspaceIdAndUserId(10L, 2L);
        verify(memberRepository).delete(target);
    }

    @Test
    void adminCanRemoveMember() {
        WorkspaceMember target = membership(user(2L, "target@example.com", "Target"), WorkspaceRole.MEMBER);
        stubMemberships(membership(actor, WorkspaceRole.ADMIN), target);
        service.removeWorkspaceMember(10L, 2L, actor.getEmail());
        verify(memberRepository).delete(target);
    }

    @ParameterizedTest
    @EnumSource(value = WorkspaceRole.class, names = {"OWNER", "ADMIN"})
    void adminCannotRemovePrivilegedMember(WorkspaceRole targetRole) {
        stubMemberships(
                membership(actor, WorkspaceRole.ADMIN),
                membership(user(2L, "target@example.com", "Target"), targetRole)
        );
        assertThrows(RuntimeException.class, () -> service.removeWorkspaceMember(10L, 2L, actor.getEmail()));
        verify(memberRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void memberCannotRemoveAnyone() {
        stubMemberships(
                membership(actor, WorkspaceRole.MEMBER),
                membership(user(2L, "target@example.com", "Target"), WorkspaceRole.MEMBER)
        );
        assertThrows(WorkspaceMemberAccessDeniedException.class,
                () -> service.removeWorkspaceMember(10L, 2L, actor.getEmail()));
    }

    @Test
    void actorCannotRemoveSelf() {
        WorkspaceMember owner = membership(actor, WorkspaceRole.OWNER);
        stubMemberships(owner, owner);
        assertThrows(WorkspaceMemberConflictException.class,
                () -> service.removeWorkspaceMember(10L, 1L, actor.getEmail()));
    }

    private void stubMemberships(WorkspaceMember actorMembership, WorkspaceMember targetMembership) {
        when(memberRepository.findByWorkspaceIdAndUserId(10L, 1L)).thenReturn(Optional.of(actorMembership));
        when(memberRepository.findByWorkspaceIdAndUserId(10L, targetMembership.getUser().getId()))
                .thenReturn(Optional.of(targetMembership));
    }

    private WorkspaceMember membership(User user, WorkspaceRole role) {
        return new WorkspaceMember(workspace, user, role, Instant.now());
    }

    private User user(Long id, String email, String displayName) {
        User user = new User(email, "hash", displayName, Instant.now(), Instant.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
