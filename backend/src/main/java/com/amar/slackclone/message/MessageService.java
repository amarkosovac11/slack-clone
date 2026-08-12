package com.amar.slackclone.message;

import com.amar.slackclone.channel.Channel;
import com.amar.slackclone.channel.ChannelMemberRepository;
import com.amar.slackclone.channel.ChannelNotFoundException;
import com.amar.slackclone.channel.ChannelRepository;
import com.amar.slackclone.channel.AuthenticatedUserNotFoundException;
import com.amar.slackclone.channel.WorkspaceAccessDeniedException;
import com.amar.slackclone.channel.WorkspaceNotFoundException;
import com.amar.slackclone.message.dto.CreateMessageRequest;
import com.amar.slackclone.message.dto.MessageResponse;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import com.amar.slackclone.workspace.WorkspaceMemberRepository;
import com.amar.slackclone.workspace.WorkspaceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public MessageService(
        MessageRepository messageRepository,
        ChannelRepository channelRepository,
        ChannelMemberRepository channelMemberRepository,
        WorkspaceRepository workspaceRepository,
        WorkspaceMemberRepository workspaceMemberRepository,
        UserRepository userRepository
    ) {
        this.messageRepository = messageRepository;
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(
        Long workspaceId,
        Long channelId,
        String authenticatedEmail
    ) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);

        validateChannelAccess(
            workspaceId,
            channelId,
            authenticatedUser.getId()
        );

        return messageRepository
            .findAllByChannelIdAndDeletedAtIsNullOrderByCreatedAtAsc(channelId)
            .stream()
            .map(this::toMessageResponse)
            .toList();
    }

    @Transactional
    public MessageResponse createMessage(
        Long workspaceId,
        Long channelId,
        CreateMessageRequest request,
        String authenticatedEmail
    ) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);

        Channel channel = validateChannelAccess(
            workspaceId,
            channelId,
            authenticatedUser.getId()
        );

        Message message = new Message();
        message.setChannel(channel);
        message.setSender(authenticatedUser);
        message.setContent(request.content().trim());

        Message savedMessage = messageRepository.save(message);

        return toMessageResponse(savedMessage);
    }

    private Channel validateChannelAccess(
        Long workspaceId,
        Long channelId,
        Long userId
    ) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException(workspaceId);
        }

        boolean workspaceMember =
            workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                workspaceId,
                userId
            );

        if (!workspaceMember) {
            throw new WorkspaceAccessDeniedException(
                "You do not have access to this workspace"
            );
        }

        Channel channel = channelRepository
            .findByIdAndWorkspaceId(channelId, workspaceId)
            .orElseThrow(() ->
                new ChannelNotFoundException(channelId)
            );

        if (
            channel.isPrivateChannel() &&
            !channelMemberRepository.existsByChannelIdAndUserId(
                channelId,
                userId
            )
        ) {
            throw new WorkspaceAccessDeniedException(
                "You do not have access to this private channel"
            );
        }

        return channel;
    }

    private User getAuthenticatedUser(String authenticatedEmail) {
        return userRepository
            .findByEmailIgnoreCase(authenticatedEmail)
            .orElseThrow(AuthenticatedUserNotFoundException::new);
    }

    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getChannel().getId(),
            message.getSender().getId(),
            message.getSender().getDisplayName(),
            message.getSender().getEmail(),
            message.getContent(),
            message.getCreatedAt(),
            message.getUpdatedAt()
        );
    }
}
