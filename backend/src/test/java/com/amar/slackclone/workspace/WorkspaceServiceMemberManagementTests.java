package com.amar.slackclone.workspace;

import com.amar.slackclone.channel.ChannelMemberRepository;
import com.amar.slackclone.channel.WorkspaceAccessDeniedException;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import com.amar.slackclone.workspace.dto.UpdateWorkspaceMemberRoleRequest;
import com.amar.slackclone.workspace.dto.UpdateWorkspaceRequest;
import com.amar.slackclone.workspace.dto.TransferWorkspaceOwnershipRequest;
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
                channelMemberRepository,
                new WorkspaceAccessService(workspaceRepository, memberRepository, userRepository)
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
        assertThrows(WorkspaceAccessDeniedException.class, () -> service.updateWorkspaceMemberRole(
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
        assertThrows(WorkspaceAccessDeniedException.class,
                () -> service.removeWorkspaceMember(10L, 2L, actor.getEmail()));
    }

    @Test
    void actorCannotRemoveSelf() {
        WorkspaceMember owner = membership(actor, WorkspaceRole.OWNER);
        stubMemberships(owner, owner);
        assertThrows(WorkspaceMemberConflictException.class,
                () -> service.removeWorkspaceMember(10L, 1L, actor.getEmail()));
    }

    @ParameterizedTest
    @EnumSource(value = WorkspaceRole.class, names = {"OWNER", "ADMIN"})
    void ownerAndAdminCanEditWorkspace(WorkspaceRole actorRole) {
        when(memberRepository.findByWorkspaceIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(membership(actor, actorRole)));

        var response = service.updateWorkspace(10L, new UpdateWorkspaceRequest("Renamed Team"), actor.getEmail());

        assertEquals("Renamed Team", response.name());
        assertEquals("Renamed Team", workspace.getName());
    }

    @Test
    void memberCannotEditWorkspace() {
        when(memberRepository.findByWorkspaceIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(membership(actor, WorkspaceRole.MEMBER)));
        assertThrows(WorkspaceAccessDeniedException.class,
                () -> service.updateWorkspace(10L, new UpdateWorkspaceRequest("Renamed"), actor.getEmail()));
    }

    @Test
    void onlyOwnerCanDeleteWorkspace() {
        WorkspaceMember owner = membership(actor, WorkspaceRole.OWNER);
        when(memberRepository.findByWorkspaceIdAndUserId(10L, 1L)).thenReturn(Optional.of(owner));

        service.deleteWorkspace(10L, actor.getEmail());

        verify(workspaceRepository).delete(workspace);
    }

    @ParameterizedTest
    @EnumSource(value = WorkspaceRole.class, names = {"ADMIN", "MEMBER"})
    void adminAndMemberCannotDeleteWorkspace(WorkspaceRole actorRole) {
        when(memberRepository.findByWorkspaceIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(membership(actor, actorRole)));
        assertThrows(WorkspaceAccessDeniedException.class,
                () -> service.deleteWorkspace(10L, actor.getEmail()));
        verify(workspaceRepository, never()).delete(workspace);
    }

    @ParameterizedTest
    @EnumSource(value = WorkspaceRole.class, names = {"ADMIN", "MEMBER"})
    void adminAndMemberCanLeaveWorkspace(WorkspaceRole role) {
        WorkspaceMember membership = membership(actor, role);
        when(memberRepository.findByWorkspaceIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        service.leaveWorkspace(10L, actor.getEmail());
        verify(channelMemberRepository).deleteAllByWorkspaceIdAndUserId(10L, 1L);
        verify(memberRepository).delete(membership);
        verify(userRepository, never()).delete(actor);
        verify(workspaceRepository, never()).delete(workspace);
    }

    @Test
    void ownerCannotLeaveWorkspace() {
        WorkspaceMember membership = membership(actor, WorkspaceRole.OWNER);
        when(memberRepository.findByWorkspaceIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        assertThrows(WorkspaceMemberConflictException.class, () -> service.leaveWorkspace(10L, actor.getEmail()));
        verify(memberRepository, never()).delete(membership);
    }
    @Test void ownershipTransferPromotesTargetAndDemotesOldOwner(){WorkspaceMember owner=membership(actor,WorkspaceRole.OWNER);User next=user(2L,"next@example.com","Next");WorkspaceMember target=membership(next,WorkspaceRole.MEMBER);stubMemberships(owner,target);when(workspaceRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(workspace));var response=service.transferOwnership(10L,new TransferWorkspaceOwnershipRequest(2L),actor.getEmail());assertEquals(WorkspaceRole.ADMIN,owner.getRole());assertEquals(WorkspaceRole.OWNER,target.getRole());assertEquals(next,workspace.getOwner());assertEquals(WorkspaceRole.ADMIN,response.currentUserRole());}

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
