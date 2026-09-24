package com.amar.slackclone.message;

import com.amar.slackclone.channel.Channel;
import com.amar.slackclone.messaging.event.MessageCreatedEvent;
import com.amar.slackclone.messaging.producer.MessageEventProducer;
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
import com.amar.slackclone.notification.*;
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
    private final ChannelMessageReactionRepository reactions; private final ChannelMessageAttachmentRepository attachments;
    private final NotificationService notificationService;
    private final MessageEventProducer messageEventProducer;

    public MessageService(
        MessageRepository messageRepository,
        ChannelAccessService channelAccessService,
        UserRepository userRepository,
        SimpMessagingTemplate messagingTemplate, ChannelPinnedMessageRepository pins, ChannelMessageMentionRepository mentions,
        WorkspaceMemberRepository workspaceMembers, ChannelMemberRepository channelMembers,ChannelMessageReactionRepository reactions,ChannelMessageAttachmentRepository attachments,
        NotificationService notificationService, MessageEventProducer messageEventProducer
    ) {
        this.messageRepository = messageRepository;
        this.channelAccessService = channelAccessService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.pins=pins;this.mentions=mentions;this.workspaceMembers=workspaceMembers;this.channelMembers=channelMembers;
        this.reactions=reactions;this.attachments=attachments;
        this.notificationService=notificationService;
        this.messageEventProducer = messageEventProducer;
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
            .map(message -> toMessageResponse(message, getAuthenticatedUser(authenticatedEmail).getId()))
            .toList();
    }
    @Transactional(readOnly=true) public com.amar.slackclone.message.dto.MessageContextResponse messageContext(Long workspaceId,Long channelId,Long messageId,String email){
        channelAccessService.validateChannelAccess(workspaceId,channelId,email);Message target=requireMessage(channelId,messageId);Long viewerId=getAuthenticatedUser(email).getId();
        List<MessageResponse> context=messageRepository.findAllByChannelIdOrderByCreatedAtAsc(channelId).stream().map(message->toMessageResponse(message,viewerId)).toList();
        return new com.amar.slackclone.message.dto.MessageContextResponse(messageId,target.getThreadRootMessage()==null?null:target.getThreadRootMessage().getId(),context);
    }

    @Transactional
    public MessageResponse createMessage(
        Long workspaceId,
        Long channelId,
        CreateMessageRequest request,
        String authenticatedEmail
    ) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);

        Channel channel = channelAccessService.validateChannelWriteAccess(
            workspaceId,
            channelId,
            authenticatedEmail
        );

        Message message = new Message();
        message.setChannel(channel);
        message.setSender(authenticatedUser);
        message.setContent(request.content().trim());

        message=messageRepository.saveAndFlush(message); persistMentions(message,true); MessageResponse response = toMessageResponse(message);
        publishCreatedAfterCommit(workspaceId, response, null);
        broadcastAfterCommit(workspaceId, channelId, ChannelMessageEventType.MESSAGE_CREATED, response, null);
        return response;
    }

    @Transactional
    public MessageResponse updateMessage(Long workspaceId, Long channelId, Long messageId,
            UpdateMessageRequest request, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        channelAccessService.validateChannelWriteAccess(workspaceId, channelId, authenticatedEmail);
        Message message = requireMessage(channelId, messageId);
        requireSender(message, user);
        requireNotDeleted(message);
        message.setContent(request.content().trim());
        mentions.deleteAllByMessageId(messageId); persistMentions(message,false);
        message.markUpdated();
        MessageResponse response = toMessageResponse(message);
        broadcastAfterCommit(workspaceId, channelId, ChannelMessageEventType.MESSAGE_UPDATED, response, null);
        return response;
    }

    @Transactional
    public MessageResponse deleteMessage(Long workspaceId, Long channelId, Long messageId,
            String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        channelAccessService.validateChannelWriteAccess(workspaceId, channelId, authenticatedEmail);
        Message message = requireMessage(channelId, messageId);
        requireSender(message, user);
        requireNotDeleted(message);
        message.setDeletedAt(java.time.OffsetDateTime.now());
        mentions.deleteAllByMessageId(messageId); pins.deleteByMessageId(messageId);
        message.markUpdated();
        MessageResponse response = toMessageResponse(message);
        broadcastAfterCommit(workspaceId, channelId, ChannelMessageEventType.MESSAGE_DELETED, response, null);
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

    private void publishCreatedAfterCommit(Long workspaceId, MessageResponse response, MessageResponse threadRoot) {
        MessageCreatedEvent event = new MessageCreatedEvent(workspaceId, response, threadRoot);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messageEventProducer.send(event);
            }
        });
    }

    private void broadcastAfterCommit(
        Long workspaceId,
        Long channelId,
        ChannelMessageEventType type,
        MessageResponse response,
        Long threadRootMessageId
    ) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend(
                        "/topic/workspaces/%d/channels/%d/messages"
                            .formatted(workspaceId, channelId),
                        new ChannelMessageEvent(type, response, threadRootMessageId)
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

    private MessageResponse toMessageResponse(Message message) { return toMessageResponse(message,null); }
    private MessageResponse toMessageResponse(Message message,Long viewerId) {
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
            pins.existsById(message.getId()),message.getThreadRootMessage()==null?null:message.getThreadRootMessage().getId(),messageRepository.countByThreadRootMessageId(message.getId()),reactionSummaries(message.getId(),viewerId),attachmentResponses(message.getId())
        );
    }
    @Transactional public MessageResponse addReaction(Long workspaceId,Long channelId,Long messageId,String emoji,String email){channelAccessService.validateChannelWriteAccess(workspaceId,channelId,email);Message m=requireMessage(channelId,messageId);requireNotDeleted(m);User u=getAuthenticatedUser(email);reactions.findByMessageIdAndUserIdAndEmoji(messageId,u.getId(),emoji).orElseGet(()->reactions.save(new ChannelMessageReaction(m,u,emoji)));MessageResponse shared=toMessageResponse(m);broadcastAfterCommit(workspaceId,channelId,ChannelMessageEventType.REACTION_UPDATED,shared,m.getThreadRootMessage()==null?null:m.getThreadRootMessage().getId());return toMessageResponse(m,u.getId());}
    @Transactional public MessageResponse removeReaction(Long workspaceId,Long channelId,Long messageId,String emoji,String email){channelAccessService.validateChannelWriteAccess(workspaceId,channelId,email);Message m=requireMessage(channelId,messageId);User u=getAuthenticatedUser(email);reactions.findByMessageIdAndUserIdAndEmoji(messageId,u.getId(),emoji).ifPresent(reactions::delete);MessageResponse shared=toMessageResponse(m);broadcastAfterCommit(workspaceId,channelId,ChannelMessageEventType.REACTION_UPDATED,shared,m.getThreadRootMessage()==null?null:m.getThreadRootMessage().getId());return toMessageResponse(m,u.getId());}
    @Transactional(readOnly=true) public List<MessageResponse> thread(Long workspaceId,Long channelId,Long messageId,String email){channelAccessService.validateChannelAccess(workspaceId,channelId,email);Message root=requireMessage(channelId,messageId);if(root.getThreadRootMessage()!=null)root=root.getThreadRootMessage();List<MessageResponse> out=new ArrayList<>();out.add(toMessageResponse(root));out.addAll(messageRepository.findAllByThreadRootMessageIdOrderByCreatedAtAsc(root.getId()).stream().map(this::toMessageResponse).toList());return out;}
    @Transactional public MessageResponse reply(Long workspaceId,Long channelId,Long messageId,CreateMessageRequest request,String email){Channel c=channelAccessService.validateChannelWriteAccess(workspaceId,channelId,email);Message target=requireMessage(channelId,messageId);Message root=target.getThreadRootMessage()==null?target:target.getThreadRootMessage();User actor=getAuthenticatedUser(email);Message reply=new Message();reply.setChannel(c);reply.setSender(actor);reply.setContent(request.content().trim());reply.setThreadRootMessage(root);reply=messageRepository.saveAndFlush(reply);persistMentions(reply,true);notificationService.create(root.getSender().getId(),actor.getId(),NotificationType.THREAD_REPLY,actor.getDisplayName()+" replied to your thread in #"+c.getName(),new NotificationService.Context(workspaceId,channelId,null,root.getId(),null));MessageResponse rr=toMessageResponse(reply);publishCreatedAfterCommit(workspaceId,rr,toMessageResponse(root));broadcastAfterCommit(workspaceId,channelId,ChannelMessageEventType.THREAD_REPLY_CREATED,rr,root.getId());broadcastAfterCommit(workspaceId,channelId,ChannelMessageEventType.THREAD_UPDATED,toMessageResponse(root),root.getId());return rr;}
    private List<ReactionSummary> reactionSummaries(Long id,Long viewer){return reactions.findAllByMessageId(id).stream().collect(java.util.stream.Collectors.groupingBy(ChannelMessageReaction::getEmoji)).entrySet().stream().map(e->new ReactionSummary(e.getKey(),e.getValue().size(),e.getValue().stream().map(x->x.getUser().getId()).toList(),e.getValue().stream().limit(20).map(x->x.getUser().getDisplayName()).toList())).toList();}
    private List<AttachmentResponse> attachmentResponses(Long id){return attachments.findAllByMessageId(id).stream().map(a->new AttachmentResponse(a.getId(),a.getOriginalFileName(),a.getMimeType(),a.getFileSize(),"/api/attachments/channel/"+a.getId())).toList();}
    @Transactional public PinnedMessageResponse pin(Long workspaceId,Long channelId,Long messageId,String email){Channel c=channelAccessService.validateChannelWriteAccess(workspaceId,channelId,email);Message m=requireMessage(channelId,messageId);requireNotDeleted(m);User u=getAuthenticatedUser(email);ChannelPinnedMessage p=pins.findById(messageId).orElseGet(ChannelPinnedMessage::new);p.setMessage(m);p.setChannel(c);p.setPinnedBy(u);p=pins.save(p);broadcastAfterCommit(workspaceId,channelId,ChannelMessageEventType.MESSAGE_PINNED,toMessageResponse(m),null);return pinResponse(p);}
    @Transactional public void unpin(Long workspaceId,Long channelId,Long messageId,String email){channelAccessService.validateChannelWriteAccess(workspaceId,channelId,email);ChannelPinnedMessage p=pins.findByMessageIdAndChannelId(messageId,channelId).orElseThrow(()->new MessageNotFoundException(messageId));Message m=p.getMessage();pins.delete(p);pins.flush();broadcastAfterCommit(workspaceId,channelId,ChannelMessageEventType.MESSAGE_UNPINNED,toMessageResponse(m),null);}
    @Transactional(readOnly=true) public List<PinnedMessageResponse> pins(Long workspaceId,Long channelId,String email){channelAccessService.validateChannelAccess(workspaceId,channelId,email);return pins.findAllByChannelIdOrderByPinnedAtDesc(channelId).stream().map(this::pinResponse).toList();}
    private PinnedMessageResponse pinResponse(ChannelPinnedMessage p){return new PinnedMessageResponse(toMessageResponse(p.getMessage()),p.getPinnedBy().getId(),p.getPinnedBy().getDisplayName(),p.getPinnedAt());}
    private static final Pattern MENTION=Pattern.compile("(?<![\\w@])@([A-Za-z0-9._-]+)");
    private void persistMentions(Message m,boolean notify){Matcher matcher=MENTION.matcher(m.getContent());if(!matcher.find())return;List<User> eligible=m.getChannel().isPrivateChannel()?channelMembers.findAllByChannelId(m.getChannel().getId()).stream().map(x->x.getUser()).toList():workspaceMembers.findAllByWorkspaceId(m.getChannel().getWorkspace().getId()).stream().map(x->x.getUser()).toList();
      Map<String,List<User>> byHandle=eligible.stream().collect(java.util.stream.Collectors.groupingBy(u->u.getUsername().toLowerCase(Locale.ROOT)));Set<Long> seen=new HashSet<>();do{List<User> found=byHandle.get(matcher.group(1).toLowerCase(Locale.ROOT));if(found!=null&&found.size()==1&&seen.add(found.getFirst().getId())){User mentioned=found.getFirst();mentions.save(new ChannelMessageMention(m,mentioned));if(notify)notificationService.create(mentioned.getId(),m.getSender().getId(),NotificationType.MENTION,m.getSender().getDisplayName()+" mentioned you in #"+m.getChannel().getName(),new NotificationService.Context(m.getChannel().getWorkspace().getId(),m.getChannel().getId(),null,m.getId(),null));}}while(matcher.find());}
    private MentionResponse mentionResponse(User u){return new MentionResponse(u.getId(),u.getDisplayName(),u.getUsername());}

    @Transactional
    public MessageResponse attachmentAdded(Long workspaceId, Long channelId, Long messageId, String email) {
        channelAccessService.validateChannelWriteAccess(workspaceId, channelId, email);
        Message message = requireMessage(channelId, messageId);
        MessageResponse response = toMessageResponse(message);
        broadcastAfterCommit(workspaceId, channelId, ChannelMessageEventType.ATTACHMENT_ADDED, response,
            message.getThreadRootMessage() == null ? null : message.getThreadRootMessage().getId());
        return response;
    }
}
