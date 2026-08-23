package com.amar.slackclone.message;

import com.amar.slackclone.channel.Channel;
import com.amar.slackclone.channel.ChannelAccessService;
import com.amar.slackclone.channel.AuthenticatedUserNotFoundException;
import com.amar.slackclone.message.dto.CreateMessageRequest;
import com.amar.slackclone.message.dto.MessageResponse;
import com.amar.slackclone.message.dto.UpdateMessageRequest;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChannelAccessService channelAccessService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(
        MessageRepository messageRepository,
        ChannelAccessService channelAccessService,
        UserRepository userRepository,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.messageRepository = messageRepository;
        this.channelAccessService = channelAccessService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(
        Long workspaceId,
        Long channelId,
        String authenticatedEmail
    ) {
        channelAccessService.validateChannelAccess(
            workspaceId,
            channelId,
            authenticatedEmail
        );

        return messageRepository
            .findAllByChannelIdOrderByCreatedAtAsc(channelId)
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

        Channel channel = channelAccessService.validateChannelAccess(
            workspaceId,
            channelId,
            authenticatedEmail
        );

        Message message = new Message();
        message.setChannel(channel);
        message.setSender(authenticatedUser);
        message.setContent(request.content().trim());

        MessageResponse response = toMessageResponse(messageRepository.save(message));
        broadcastAfterCommit(workspaceId, channelId, response);
        return response;
    }

    @Transactional
    public MessageResponse updateMessage(Long workspaceId, Long channelId, Long messageId,
            UpdateMessageRequest request, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        channelAccessService.validateChannelAccess(workspaceId, channelId, authenticatedEmail);
        Message message = requireMessage(channelId, messageId);
        requireSender(message, user);
        requireNotDeleted(message);
        message.setContent(request.content().trim());
        message.markUpdated();
        MessageResponse response = toMessageResponse(message);
        broadcastAfterCommit(workspaceId, channelId, response);
        return response;
    }

    @Transactional
    public MessageResponse deleteMessage(Long workspaceId, Long channelId, Long messageId,
            String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        channelAccessService.validateChannelAccess(workspaceId, channelId, authenticatedEmail);
        Message message = requireMessage(channelId, messageId);
        requireSender(message, user);
        requireNotDeleted(message);
        message.setDeletedAt(java.time.OffsetDateTime.now());
        message.markUpdated();
        MessageResponse response = toMessageResponse(message);
        broadcastAfterCommit(workspaceId, channelId, response);
        return response;
    }

    private Message requireMessage(Long channelId, Long messageId) {
        return messageRepository.findByIdAndChannelId(messageId, channelId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
    }

    private void requireSender(Message message, User user) {
        if (!message.getSender().getId().equals(user.getId())) throw new MessageAccessDeniedException();
    }

    private void requireNotDeleted(Message message) {
        if (message.getDeletedAt() != null) throw new MessageConflictException("Message is already deleted");
    }

    private void broadcastAfterCommit(
        Long workspaceId,
        Long channelId,
        MessageResponse response
    ) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend(
                        "/topic/workspaces/%d/channels/%d/messages"
                            .formatted(workspaceId, channelId),
                        response
                    );
                }
            }
        );
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
            message.getDeletedAt() == null ? message.getContent() : null,
            message.getCreatedAt(),
            message.getUpdatedAt(),
            message.getDeletedAt()
        );
    }
}
