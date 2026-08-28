package com.amar.slackclone.message;

import com.amar.slackclone.channel.Channel;
import com.amar.slackclone.channel.ChannelAccessService;
import com.amar.slackclone.channel.AuthenticatedUserNotFoundException;
import com.amar.slackclone.message.dto.CreateMessageRequest;
import com.amar.slackclone.message.dto.MessageResponse;
import com.amar.slackclone.message.dto.UpdateMessageRequest;
import com.amar.slackclone.message.dto.*;
import com.amar.slackclone.channel.ChannelMemberRepository;
import com.amar.slackclone.workspace.WorkspaceMemberRepository;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.*; import java.util.regex.*;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChannelAccessService channelAccessService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChannelPinnedMessageRepository pins; private final ChannelMessageMentionRepository mentions;
    private final WorkspaceMemberRepository workspaceMembers; private final ChannelMemberRepository channelMembers;

    public MessageService(
        MessageRepository messageRepository,
        ChannelAccessService channelAccessService,
        UserRepository userRepository,
        SimpMessagingTemplate messagingTemplate, ChannelPinnedMessageRepository pins, ChannelMessageMentionRepository mentions,
        WorkspaceMemberRepository workspaceMembers, ChannelMemberRepository channelMembers
    ) {
        this.messageRepository = messageRepository;
        this.channelAccessService = channelAccessService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.pins=pins;this.mentions=mentions;this.workspaceMembers=workspaceMembers;this.channelMembers=channelMembers;
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

        message=messageRepository.saveAndFlush(message); persistMentions(message); MessageResponse response = toMessageResponse(message);
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
        mentions.deleteAllByMessageId(messageId); persistMentions(message);
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
        mentions.deleteAllByMessageId(messageId); pins.deleteByMessageId(messageId);
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
            message.getDeletedAt(), mentions.findAllByMessageId(message.getId()).stream().map(x->mentionResponse(x.getUser())).toList(),
            pins.existsById(message.getId())
        );
    }
    @Transactional public PinnedMessageResponse pin(Long workspaceId,Long channelId,Long messageId,String email){Channel c=channelAccessService.validateChannelAccess(workspaceId,channelId,email);Message m=requireMessage(channelId,messageId);requireNotDeleted(m);User u=getAuthenticatedUser(email);ChannelPinnedMessage p=pins.findById(messageId).orElseGet(ChannelPinnedMessage::new);p.setMessage(m);p.setChannel(c);p.setPinnedBy(u);p=pins.save(p);return pinResponse(p);}
    @Transactional public void unpin(Long workspaceId,Long channelId,Long messageId,String email){channelAccessService.validateChannelAccess(workspaceId,channelId,email);ChannelPinnedMessage p=pins.findByMessageIdAndChannelId(messageId,channelId).orElseThrow(()->new MessageNotFoundException(messageId));pins.delete(p);}
    @Transactional(readOnly=true) public List<PinnedMessageResponse> pins(Long workspaceId,Long channelId,String email){channelAccessService.validateChannelAccess(workspaceId,channelId,email);return pins.findAllByChannelIdOrderByPinnedAtDesc(channelId).stream().map(this::pinResponse).toList();}
    private PinnedMessageResponse pinResponse(ChannelPinnedMessage p){return new PinnedMessageResponse(toMessageResponse(p.getMessage()),p.getPinnedBy().getId(),p.getPinnedBy().getDisplayName(),p.getPinnedAt());}
    private static final Pattern MENTION=Pattern.compile("(?<![\\w@])@([A-Za-z0-9._-]+)");
    private void persistMentions(Message m){Matcher matcher=MENTION.matcher(m.getContent());if(!matcher.find())return;List<User> eligible=m.getChannel().isPrivateChannel()?channelMembers.findAllByChannelId(m.getChannel().getId()).stream().map(x->x.getUser()).toList():workspaceMembers.findAllByWorkspaceId(m.getChannel().getWorkspace().getId()).stream().map(x->x.getUser()).toList();
      Map<String,List<User>> byHandle=eligible.stream().collect(java.util.stream.Collectors.groupingBy(u->handle(u).toLowerCase(Locale.ROOT)));Set<Long> seen=new HashSet<>();do{List<User> found=byHandle.get(matcher.group(1).toLowerCase(Locale.ROOT));if(found!=null&&found.size()==1&&seen.add(found.getFirst().getId()))mentions.save(new ChannelMessageMention(m,found.getFirst()));}while(matcher.find());}
    private String handle(User u){return u.getEmail().substring(0,u.getEmail().indexOf('@'));}
    private MentionResponse mentionResponse(User u){return new MentionResponse(u.getId(),u.getDisplayName(),handle(u));}
}
