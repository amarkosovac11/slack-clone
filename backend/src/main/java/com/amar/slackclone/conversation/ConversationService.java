package com.amar.slackclone.conversation;

import com.amar.slackclone.conversation.dto.*;
import com.amar.slackclone.user.*;
import com.amar.slackclone.workspace.WorkspaceMemberRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ConversationService {
    private final ConversationRepository conversations;
    private final ConversationParticipantRepository participants;
    private final ConversationMessageRepository messages;
    private final UserRepository users;
    private final WorkspaceMemberRepository workspaceMembers;
    private final ConversationAccessService access;
    private final SimpMessagingTemplate broker;
    private final ConversationMessageMentionRepository mentionRepository;
    private final ConversationMessageReactionRepository reactionRepository; private final ConversationMessageAttachmentRepository attachmentRepository;

    public ConversationService(ConversationRepository conversations, ConversationParticipantRepository participants,
            ConversationMessageRepository messages, UserRepository users, WorkspaceMemberRepository workspaceMembers,
            ConversationAccessService access, SimpMessagingTemplate broker, ConversationMessageMentionRepository mentionRepository,ConversationMessageReactionRepository reactionRepository,ConversationMessageAttachmentRepository attachmentRepository) {
        this.conversations = conversations; this.participants = participants; this.messages = messages;
        this.users = users; this.workspaceMembers = workspaceMembers; this.access = access; this.broker = broker; this.mentionRepository=mentionRepository;this.reactionRepository=reactionRepository;this.attachmentRepository=attachmentRepository;
    }

    @Transactional
    public ConversationResponse startDirect(StartDirectConversationRequest request, String email) {
        User creator = access.requireUser(email);
        if (creator.getId().equals(request.userId())) throw new ConversationValidationException("You cannot message yourself");
        User other = users.findById(request.userId()).orElseThrow(() -> new com.amar.slackclone.channel.UserNotFoundException(request.userId()));
        requireSharedWorkspace(creator.getId(), other.getId());
        String key = directKey(creator.getId(), other.getId());
        Optional<Long> insertedId = conversations.insertDirect(creator.getId(), key);
        Conversation conversation = insertedId.isPresent()
                ? conversations.findById(insertedId.get()).orElseThrow()
                : conversations.findByDirectKey(key).orElseThrow();
        if (insertedId.isPresent()) {
            addParticipant(conversation, creator); addParticipant(conversation, other);
            participants.flush();
        } else {
            participants.findByConversationIdAndUserId(conversation.getId(), creator.getId())
                    .orElseThrow().setHiddenAt(null);
        }
        ConversationResponse result = response(conversation, creator);
        broadcastListUpdatesAfterCommit(List.of(new UserConversationUpdate(creator.getId(), result)));
        return result;
    }

    @Transactional
    public ConversationResponse createGroup(CreateGroupConversationRequest request, String email) {
        User creator = access.requireUser(email);
        LinkedHashSet<Long> ids = new LinkedHashSet<>(request.participantIds());
        ids.remove(creator.getId());
        if (ids.size() < 2) throw new ConversationValidationException("A group conversation requires at least two other users");
        if (ids.size() > 8) throw new ConversationValidationException("A group conversation supports at most 9 participants");
        List<User> selected = ids.stream().map(id -> users.findById(id)
                .orElseThrow(() -> new com.amar.slackclone.channel.UserNotFoundException(id))).toList();
        selected.forEach(user -> requireSharedWorkspace(creator.getId(), user.getId()));
        Conversation conversation = new Conversation(); conversation.setType(ConversationType.GROUP); conversation.setCreatedBy(creator);
        conversation = conversations.save(conversation); addParticipant(conversation, creator);
        for (User user : selected) addParticipant(conversation, user);
        participants.flush();
        return response(conversation, creator);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(String email) {
        User user = access.requireUser(email);
        return conversations.findAllForUser(user.getId()).stream().map(c -> response(c, user)).toList();
    }
    @Transactional(readOnly=true) public List<ConversationResponse> hidden(String email){User u=access.requireUser(email);return conversations.findHiddenForUser(u.getId()).stream().map(c->response(c,u)).toList();}
    @Transactional public ConversationResponse restore(Long id,String email){ConversationParticipant p=access.requireParticipant(id,email);p.setHiddenAt(null);ConversationResponse r=response(p.getConversation(),p.getUser());broadcastListUpdatesAfterCommit(List.of(new UserConversationUpdate(p.getUser().getId(),r)));return r;}

    @Transactional(readOnly = true)
    public ConversationResponse get(Long id, String email) {
        ConversationParticipant membership = access.requireParticipant(id, email);
        return response(membership.getConversation(), membership.getUser());
    }

    @Transactional(readOnly = true)
    public List<ConversationUserResponse> eligibleUsers(String email) {
        User user = access.requireUser(email);
        return workspaceMembers.findMessageableUsers(user.getId()).stream().map(this::userResponse).toList();
    }

    @Transactional(readOnly = true)
    public ConversationMessagePageResponse history(Long id, Long before, int requestedLimit, String email) {
        ConversationParticipant membership = access.requireParticipant(id, email);
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        var page = PageRequest.of(0, limit + 1);
        List<ConversationMessage> descending = before == null
                ? messages.findByConversationIdAndCreatedAtGreaterThanEqualOrderByIdDesc(id, membership.getJoinedAt(), page)
                : messages.findByConversationIdAndIdLessThanAndCreatedAtGreaterThanEqualOrderByIdDesc(id, before, membership.getJoinedAt(), page);
        boolean hasMore = descending.size() > limit;
        if (hasMore) descending = new ArrayList<>(descending.subList(0, limit));
        Long next = hasMore ? descending.get(descending.size() - 1).getId() : null;
        Collections.reverse(descending);
        return new ConversationMessagePageResponse(descending.stream().map(this::messageResponse).toList(), next);
    }

    @Transactional
    public ConversationMessageResponse send(Long id, CreateConversationMessageRequest request, String email) {
        ConversationParticipant senderMembership = access.requireParticipant(id, email);
        Conversation conversation = senderMembership.getConversation();
        if (conversation.getType() == ConversationType.GROUP && participants.countByConversationIdAndLeftAtIsNull(id) < 2)
            throw new ConversationValidationException("A group needs at least two active participants to send messages");
        ConversationMessage message = new ConversationMessage(); message.setConversation(conversation);
        message.setSender(senderMembership.getUser()); message.setContent(request.content().trim());
        message = messages.saveAndFlush(message); persistMentions(message); senderMembership.setLastReadMessage(message); conversation.touch();
        participants.findAllByConversationIdAndLeftAtIsNullOrderByJoinedAt(id).forEach(participant -> participant.setHiddenAt(null));
        ConversationMessageResponse result = messageResponse(message);
        List<UserConversationUpdate> updates = conversationUpdates(conversation);
        broadcastAfterCommit(new ConversationMessageEvent(ConversationMessageEventType.CREATED, result), updates);
        return result;
    }

    @Transactional
    public ConversationResponse rename(Long id, UpdateConversationRequest request, String email) {
        ConversationParticipant participant = access.requireParticipant(id, email);
        Conversation conversation = participant.getConversation();
        if (conversation.getType() != ConversationType.GROUP)
            throw new ConversationValidationException("Direct conversations cannot be renamed");
        String name = request.name() == null ? null : request.name().trim();
        conversation.setCustomName(name == null || name.isEmpty() ? null : name);
        ConversationResponse result = response(conversation, participant.getUser());
        broadcastListUpdatesAfterCommit(conversationUpdates(conversation));
        return result;
    }

    @Transactional
    public ConversationMessageResponse editMessage(Long id, Long messageId,
            UpdateConversationMessageRequest request, String email) {
        ConversationParticipant participant = access.requireParticipant(id, email);
        ConversationMessage message = requireMessage(id, messageId);
        requireSender(message, participant.getUser());
        if (message.getDeletedAt() != null) throw new ConversationValidationException("Deleted messages cannot be edited");
        String content = request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) throw new ConversationValidationException("Message content cannot be empty");
        message.edit(content);
        mentionRepository.deleteAllByMessageId(messageId); persistMentions(message);
        ConversationMessageResponse result = messageResponse(message);
        broadcastAfterCommit(new ConversationMessageEvent(ConversationMessageEventType.UPDATED, result),
                conversationUpdates(participant.getConversation()));
        return result;
    }

    @Transactional
    public ConversationMessageResponse deleteMessage(Long id, Long messageId, String email) {
        ConversationParticipant participant = access.requireParticipant(id, email);
        ConversationMessage message = requireMessage(id, messageId);
        requireSender(message, participant.getUser());
        if (message.getDeletedAt() != null) throw new ConversationValidationException("Message is already deleted");
        message.softDelete();
        mentionRepository.deleteAllByMessageId(messageId);
        ConversationMessageResponse result = messageResponse(message);
        broadcastAfterCommit(new ConversationMessageEvent(ConversationMessageEventType.DELETED, result),
                conversationUpdates(participant.getConversation()));
        return result;
    }

    @Transactional
    public void hide(Long id, String email) {
        ConversationParticipant participant = access.requireParticipant(id, email);
        participant.setHiddenAt(java.time.OffsetDateTime.now());
        Long userId = participant.getUser().getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { broker.convertAndSend("/topic/users/" + userId + "/conversations",
                    ConversationListEvent.removed(id)); }
        });
    }

    @Transactional
    public ConversationResponse markRead(Long id, String email) {
        ConversationParticipant participant = access.requireParticipant(id, email);
        messages.findTopByConversationIdAndCreatedAtGreaterThanEqualOrderByIdDesc(id, participant.getJoinedAt()).ifPresent(participant::setLastReadMessage);
        ConversationResponse result = response(participant.getConversation(), participant.getUser());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { broker.convertAndSend(
                    "/topic/users/" + participant.getUser().getId() + "/conversations", ConversationListEvent.upsert(result));
                participants.findAllByConversationIdAndLeftAtIsNullOrderByJoinedAt(id).forEach(member -> broker.convertAndSend(
                    "/topic/users/"+member.getUser().getId()+"/conversations/"+id+"/metadata",new ConversationMetadataEvent("READ_UPDATED",id,participant.getUser().getId()))); }
        });
        return result;
    }
    @Transactional(readOnly=true)
    public ConversationReadReceiptResponse receipt(Long id,Long messageId,String email){access.requireParticipant(id,email);ConversationMessage m=requireMessage(id,messageId);
        var eligible=participants.findAllByConversationIdAndLeftAtIsNullOrderByJoinedAt(id).stream().filter(p->!p.getUser().getId().equals(m.getSender().getId())&&!p.getJoinedAt().isAfter(m.getCreatedAt())).toList();
        var readers=eligible.stream().filter(p->p.getLastReadMessage()!=null&&p.getLastReadMessage().getId()>=m.getId()).map(p->p.getUser().getDisplayName()).toList();
        return new ConversationReadReceiptResponse(messageId,readers.size(),eligible.size(),readers);}

    @Transactional public ConversationResponse transferCreator(Long id,TransferConversationCreatorRequest request,String email){ConversationParticipant actor=access.requireParticipant(id,email);Conversation c=requireGroup(actor.getConversation());
        if(!c.getCreatedBy().getId().equals(actor.getUser().getId()))throw new ConversationAccessDeniedException("Only the current creator can transfer creator rights");
        ConversationParticipant target=participants.findByConversationIdAndUserId(id,request.newCreatorUserId()).filter(ConversationParticipant::isActive).orElseThrow(()->new ConversationValidationException("New creator must be an active participant"));
        if(target.getUser().getId().equals(actor.getUser().getId()))throw new ConversationValidationException("Select another participant");c.setCreatedBy(target.getUser());c.touch();
        ConversationResponse result=response(c,actor.getUser());broadcastMembershipAfterCommit(c,"CREATOR_TRANSFERRED",target.getUser().getId());return result;}

    @Transactional(readOnly = true)
    public List<ConversationParticipantResponse> participants(Long id, String email) {
        Conversation conversation = requireGroup(access.requireParticipant(id, email).getConversation());
        return participants.findAllByConversationIdAndLeftAtIsNullOrderByJoinedAt(id).stream()
                .map(member -> new ConversationParticipantResponse(member.getUser().getId(), member.getUser().getDisplayName(),
                        null, member.getJoinedAt(), member.getUser().getId().equals(conversation.getCreatedBy().getId()) ? "CREATOR" : "MEMBER"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationUserResponse> eligibleParticipants(Long id, String email) {
        ConversationParticipant requester = access.requireParticipant(id, email);
        requireGroup(requester.getConversation());
        Set<Long> activeIds = new HashSet<>(participants.findUserIds(id));
        return workspaceMembers.findMessageableUsers(requester.getUser().getId()).stream()
                .filter(user -> !activeIds.contains(user.getId()) && !user.getId().equals(requester.getUser().getId()))
                .map(this::userResponse).toList();
    }

    @Transactional
    public ConversationResponse addParticipants(Long id, AddConversationParticipantsRequest request, String email) {
        ConversationParticipant requester = access.requireParticipant(id, email);
        Conversation conversation = requireGroup(requester.getConversation());
        LinkedHashSet<Long> ids = new LinkedHashSet<>(request.userIds());
        if (ids.size() != request.userIds().size()) throw new ConversationValidationException("Duplicate users are not allowed");
        if (ids.contains(requester.getUser().getId())) throw new ConversationValidationException("You are already in this group");
        long activeCount = participants.countByConversationIdAndLeftAtIsNull(id);
        if (activeCount + ids.size() > 9) throw new ConversationValidationException("A group conversation supports at most 9 participants");
        OffsetDateTime now = OffsetDateTime.now();
        for (Long userId : ids) {
            User target = users.findById(userId).orElseThrow(() -> new com.amar.slackclone.channel.UserNotFoundException(userId));
            requireSharedWorkspace(requester.getUser().getId(), target.getId());
            Optional<ConversationParticipant> existing = participants.findByConversationIdAndUserId(id, userId);
            if (existing.isPresent() && existing.get().isActive())
                throw new ConversationValidationException(target.getDisplayName() + " is already in this group");
            ConversationParticipant membership = existing.orElseGet(() -> {
                ConversationParticipant value = new ConversationParticipant(); value.setConversation(conversation); value.setUser(target); return value;
            });
            membership.setJoinedAt(now); membership.setLeftAt(null); membership.setHiddenAt(null); membership.setLastReadMessage(null);
            participants.save(membership);
        }
        conversation.touch(); participants.flush();
        ConversationResponse result = response(conversation, requester.getUser());
        broadcastMembershipAfterCommit(conversation, "PARTICIPANT_ADDED", null);
        return result;
    }

    @Transactional
    public void removeParticipant(Long id, Long userId, String email) {
        ConversationParticipant requester = access.requireParticipant(id, email);
        Conversation conversation = requireGroup(requester.getConversation());
        if (!requester.getUser().getId().equals(conversation.getCreatedBy().getId()))
            throw new ConversationAccessDeniedException("Only the group creator can remove participants");
        if (userId.equals(requester.getUser().getId()))
            throw new ConversationValidationException("Use Leave group to leave your own group");
        ConversationParticipant target = participants.findByConversationIdAndUserId(id, userId)
                .filter(ConversationParticipant::isActive)
                .orElseThrow(() -> new ConversationValidationException("User is not an active participant"));
        deactivate(target); conversation.touch();
        broadcastMembershipAfterCommit(conversation, "PARTICIPANT_REMOVED", userId);
    }

    @Transactional
    public void leave(Long id, String email) {
        ConversationParticipant requester = access.requireParticipant(id, email);
        Conversation conversation = requireGroup(requester.getConversation());
        long activeCount = participants.countByConversationIdAndLeftAtIsNull(id);
        if (requester.getUser().getId().equals(conversation.getCreatedBy().getId()) && activeCount > 1)
            throw new ConversationValidationException("Transfer of group ownership is not supported yet");
        deactivate(requester); conversation.touch();
        broadcastMembershipAfterCommit(conversation, "PARTICIPANT_LEFT", requester.getUser().getId());
    }

    private Conversation requireGroup(Conversation conversation) {
        if (conversation.getType() != ConversationType.GROUP)
            throw new ConversationValidationException("Participant management is only available for group conversations");
        return conversation;
    }

    private void deactivate(ConversationParticipant participant) {
        participant.setLeftAt(OffsetDateTime.now()); participant.setHiddenAt(OffsetDateTime.now()); participant.setLastReadMessage(null);
    }

    private void broadcastMembershipAfterCommit(Conversation conversation, String type, Long removedUserId) {
        List<UserConversationUpdate> activeUpdates = conversationUpdates(conversation);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                if (removedUserId != null) broker.convertAndSend("/topic/users/" + removedUserId + "/conversations",
                        ConversationListEvent.removed(conversation.getId()));
                broadcastListUpdates(activeUpdates);
                activeUpdates.forEach(update -> broker.convertAndSend("/topic/users/" + update.userId()
                        + "/conversations/" + conversation.getId() + "/metadata",
                        new ConversationMetadataEvent(type, conversation.getId(), removedUserId)));
            }
        });
    }

    private void requireSharedWorkspace(Long first, Long second) {
        if (!workspaceMembers.shareWorkspace(first, second))
            throw new ConversationAccessDeniedException("You can only message users who share a workspace with you");
    }
    private void addParticipant(Conversation conversation, User user) {
        ConversationParticipant p = new ConversationParticipant(); p.setConversation(conversation); p.setUser(user); participants.save(p);
    }
    private String directKey(Long a, Long b) { return Math.min(a, b) + ":" + Math.max(a, b); }
    private ConversationResponse response(Conversation conversation, User viewer) {
        List<ConversationUserResponse> people = participants.findAllByConversationIdAndLeftAtIsNullOrderByJoinedAt(conversation.getId()).stream()
                .map(ConversationParticipant::getUser).map(this::userResponse).toList();
        var membership = participants.findByConversationIdAndUserId(conversation.getId(), viewer.getId()).orElseThrow();
        long after = membership.getLastReadMessage() == null ? 0L : membership.getLastReadMessage().getId();
        ConversationMessageResponse last = messages.findTopByConversationIdAndCreatedAtGreaterThanEqualOrderByIdDesc(
                conversation.getId(), membership.getJoinedAt()).map(this::messageResponse).orElse(null);
        return response(conversation, viewer, people, last, messages.countUnreadSince(
                conversation.getId(), membership.getJoinedAt(), after, viewer.getId()));
    }
    private ConversationResponse response(Conversation conversation, User viewer, List<ConversationUserResponse> people,
            ConversationMessageResponse last, long unread) {
        String generatedName = people.stream().filter(p -> !p.id().equals(viewer.getId())).map(ConversationUserResponse::displayName)
                .reduce((a, b) -> a + ", " + b).orElse("Direct message");
        String name = conversation.getType() == ConversationType.GROUP && conversation.getCustomName() != null
                ? conversation.getCustomName() : generatedName;
        return new ConversationResponse(conversation.getId(), conversation.getType(), people, conversation.getCustomName(), name, last, unread,
                conversation.getCreatedAt(), conversation.getUpdatedAt());
    }
    private ConversationUserResponse userResponse(User u) { return new ConversationUserResponse(u.getId(), u.getDisplayName(), u.getEmail()); }
    private ConversationMessageResponse messageResponse(ConversationMessage m) {
        return new ConversationMessageResponse(m.getId(), m.getConversation().getId(), m.getSender().getId(),
                m.getSender().getDisplayName(), m.getContent(), m.getCreatedAt(), m.getUpdatedAt(), m.getDeletedAt(), mentionRepository.findAllByMessageId(m.getId()).stream().map(x -> {
                    User u=x.getUser(); return new com.amar.slackclone.message.dto.MentionResponse(u.getId(),u.getDisplayName(),handle(u)); }).toList(),m.getThreadRootMessage()==null?null:m.getThreadRootMessage().getId(),messages.countByThreadRootMessageId(m.getId()),conversationReactions(m.getId()),conversationAttachments(m.getId()));
    }
    private ConversationMessage requireMessage(Long conversationId, Long messageId) {
        return messages.findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }
    private void requireSender(ConversationMessage message, User user) {
        if (!message.getSender().getId().equals(user.getId()))
            throw new ConversationAccessDeniedException("Only the sender can modify this message");
    }
    private List<UserConversationUpdate> conversationUpdates(Conversation conversation) {
        return participants.findAllByConversationIdAndLeftAtIsNullOrderByJoinedAt(conversation.getId()).stream()
                .filter(participant -> participant.getHiddenAt() == null)
                .map(ConversationParticipant::getUser)
                .map(user -> new UserConversationUpdate(user.getId(), response(conversation, user))).toList();
    }
    private record UserConversationUpdate(Long userId, ConversationResponse response) {}
    private void broadcastAfterCommit(ConversationMessageEvent event, List<UserConversationUpdate> updates) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                updates.forEach(update -> broker.convertAndSend("/topic/users/" + update.userId()
                        + "/conversations/" + event.message().conversationId() + "/messages", event));
                broadcastListUpdates(updates);
            }
        });
    }
    private void broadcastListUpdatesAfterCommit(List<UserConversationUpdate> updates) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { broadcastListUpdates(updates); }
        });
    }
    private void broadcastListUpdates(List<UserConversationUpdate> updates) {
        updates.forEach(update -> broker.convertAndSend("/topic/users/" + update.userId() + "/conversations",
                ConversationListEvent.upsert(update.response())));
    }
    private static final java.util.regex.Pattern MENTION=java.util.regex.Pattern.compile("(?<![\\w@])@([A-Za-z0-9._-]+)");
    private void persistMentions(ConversationMessage message) { var eligible=participants.findAllByConversationIdAndLeftAtIsNullOrderByJoinedAt(message.getConversation().getId()).stream().map(ConversationParticipant::getUser).toList(); var grouped=eligible.stream().collect(java.util.stream.Collectors.groupingBy(u->handle(u).toLowerCase(java.util.Locale.ROOT))); var matcher=MENTION.matcher(message.getContent()); Set<Long> seen=new HashSet<>(); while(matcher.find()){var found=grouped.get(matcher.group(1).toLowerCase(java.util.Locale.ROOT));if(found!=null&&found.size()==1&&seen.add(found.getFirst().getId()))mentionRepository.save(new ConversationMessageMention(message,found.getFirst()));}}
    private String handle(User u){return u.getEmail().substring(0,u.getEmail().indexOf('@'));}
    @Transactional public ConversationMessageResponse addReaction(Long id,Long messageId,String emoji,String email){ConversationParticipant p=access.requireParticipant(id,email);ConversationMessage m=requireMessage(id,messageId);if(m.getDeletedAt()!=null)throw new ConversationValidationException("Deleted messages cannot receive reactions");reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId,p.getUser().getId(),emoji).orElseGet(()->reactionRepository.save(new ConversationMessageReaction(m,p.getUser(),emoji)));ConversationMessageResponse r=messageResponse(m);broadcastAfterCommit(new ConversationMessageEvent(ConversationMessageEventType.UPDATED,r),conversationUpdates(p.getConversation()));return r;}
    @Transactional public ConversationMessageResponse removeReaction(Long id,Long messageId,String emoji,String email){ConversationParticipant p=access.requireParticipant(id,email);ConversationMessage m=requireMessage(id,messageId);reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId,p.getUser().getId(),emoji).ifPresent(reactionRepository::delete);ConversationMessageResponse r=messageResponse(m);broadcastAfterCommit(new ConversationMessageEvent(ConversationMessageEventType.UPDATED,r),conversationUpdates(p.getConversation()));return r;}
    @Transactional(readOnly=true) public List<ConversationMessageResponse> thread(Long id,Long messageId,String email){ConversationParticipant p=access.requireParticipant(id,email);ConversationMessage target=requireMessage(id,messageId),root=target.getThreadRootMessage()==null?target:target.getThreadRootMessage();if(root.getCreatedAt().isBefore(p.getJoinedAt()))throw new ConversationAccessDeniedException("Thread predates your membership");List<ConversationMessageResponse> out=new ArrayList<>();out.add(messageResponse(root));out.addAll(messages.findAllByThreadRootMessageIdAndCreatedAtGreaterThanEqualOrderByCreatedAt(root.getId(),p.getJoinedAt()).stream().map(this::messageResponse).toList());return out;}
    @Transactional public ConversationMessageResponse reply(Long id,Long messageId,CreateConversationMessageRequest req,String email){ConversationParticipant p=access.requireParticipant(id,email);ConversationMessage target=requireMessage(id,messageId),root=target.getThreadRootMessage()==null?target:target.getThreadRootMessage();if(root.getCreatedAt().isBefore(p.getJoinedAt()))throw new ConversationAccessDeniedException("Thread predates your membership");ConversationMessage r=new ConversationMessage();r.setConversation(p.getConversation());r.setSender(p.getUser());r.setContent(req.content().trim());r.setThreadRootMessage(root);r=messages.saveAndFlush(r);persistMentions(r);ConversationMessageResponse result=messageResponse(r);broadcastAfterCommit(new ConversationMessageEvent(ConversationMessageEventType.UPDATED,messageResponse(root)),conversationUpdates(p.getConversation()));return result;}
    private List<com.amar.slackclone.message.dto.ReactionSummary> conversationReactions(Long id){return reactionRepository.findAllByMessageId(id).stream().collect(java.util.stream.Collectors.groupingBy(ConversationMessageReaction::getEmoji)).entrySet().stream().map(e->new com.amar.slackclone.message.dto.ReactionSummary(e.getKey(),e.getValue().size(),false,e.getValue().stream().limit(20).map(x->x.getUser().getDisplayName()).toList())).toList();}
    private List<com.amar.slackclone.message.dto.AttachmentResponse> conversationAttachments(Long id){return attachmentRepository.findAllByMessageId(id).stream().map(a->new com.amar.slackclone.message.dto.AttachmentResponse(a.getId(),a.getOriginalFileName(),a.getMimeType(),a.getFileSize(),"/api/attachments/conversation/"+a.getId())).toList();}
}
