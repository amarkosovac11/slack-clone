package com.amar.slackclone.channel;

import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import com.amar.slackclone.workspace.WorkspaceMemberRepository;
import com.amar.slackclone.workspace.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelAccessService {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public ChannelAccessService(
        ChannelRepository channelRepository,
        ChannelMemberRepository channelMemberRepository,
        WorkspaceRepository workspaceRepository,
        WorkspaceMemberRepository workspaceMemberRepository,
        UserRepository userRepository
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Channel validateChannelAccess(
        Long workspaceId,
        Long channelId,
        String authenticatedEmail
    ) {
        User authenticatedUser = userRepository
            .findByEmailIgnoreCase(authenticatedEmail)
            .orElseThrow(AuthenticatedUserNotFoundException::new);

        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException(workspaceId);
        }

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(
            workspaceId,
            authenticatedUser.getId()
        )) {
            throw new WorkspaceAccessDeniedException(
                "You do not have access to this workspace"
            );
        }

        Channel channel = channelRepository
            .findByIdAndWorkspaceId(channelId, workspaceId)
            .orElseThrow(() -> new ChannelNotFoundException(channelId));

        if (channel.isPrivateChannel()
            && !channelMemberRepository.existsByChannelIdAndUserId(
                channelId,
                authenticatedUser.getId()
            )) {
            throw new WorkspaceAccessDeniedException(
                "You do not have access to this private channel"
            );
        }

        return channel;
    }
}
