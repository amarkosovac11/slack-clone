package com.amar.slackclone.conversation;

import com.amar.slackclone.conversation.dto.*;
import com.amar.slackclone.user.*;
import com.amar.slackclone.workspace.WorkspaceMemberRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
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

    public ConversationService(ConversationRepository conversations, ConversationParticipantRepository participants,
            ConversationMessageRepository messages, UserRepository users, WorkspaceMemberRepository workspaceMembers,
            ConversationAccessService access, SimpMessagingTemplate broker) {
        this.conversations = conversations; this.participants = participants; this.messages = messages;
        this.users = users; this.workspaceMembers = workspaceMembers; this.access = access; this.broker = broker;
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
        }
        return response(conversation, creator);
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
        List<Conversation> result = conversations.findAllForUser(user.getId());
        if (result.isEmpty()) return List.of();
        List<Long> ids = result.stream().map(Conversation::getId).toList();
        Map<Long, List<ConversationUserResponse>> people = participants.findAllByConversationIdIn(ids).stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> p.getConversation().getId(),
                        java.util.stream.Collectors.mapping(p -> userResponse(p.getUser()), java.util.stream.Collectors.toList())));
        Map<Long, ConversationMessageResponse> latest = messages.findLatest(ids).stream()
                .collect(java.util.stream.Collectors.toMap(m -> m.getConversation().getId(), this::messageResponse));
        Map<Long, Long> unread = messages.findUnreadCounts(user.getId()).stream()
                .collect(java.util.stream.Collectors.toMap(ConversationMessageRepository.UnreadCount::getConversationId,
                        ConversationMessageRepository.UnreadCount::getUnreadCount));
        return result.stream().map(c -> response(c, user, people.getOrDefault(c.getId(), List.of()),
                latest.get(c.getId()), unread.getOrDefault(c.getId(), 0L))).toList();
    }

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
        access.requireParticipant(id, email);
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        var page = PageRequest.of(0, limit + 1);
        List<ConversationMessage> descending = before == null
                ? messages.findByConversationIdOrderByIdDesc(id, page)
                : messages.findByConversationIdAndIdLessThanOrderByIdDesc(id, before, page);
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
        ConversationMessage message = new ConversationMessage(); message.setConversation(conversation);
        message.setSender(senderMembership.getUser()); message.setContent(request.content().trim());
        message = messages.saveAndFlush(message); senderMembership.setLastReadMessage(message); conversation.touch();
        ConversationMessageResponse result = messageResponse(message);
        List<UserConversationUpdate> updates = participants.findUserIds(id).stream()
                .map(users::findById).flatMap(Optional::stream)
                .map(user -> new UserConversationUpdate(user.getId(), response(conversation, user))).toList();
        broadcastAfterCommit(result, updates);
        return result;
    }

    @Transactional
    public ConversationResponse markRead(Long id, String email) {
        ConversationParticipant participant = access.requireParticipant(id, email);
        messages.findTopByConversationIdOrderByIdDesc(id).ifPresent(participant::setLastReadMessage);
        ConversationResponse result = response(participant.getConversation(), participant.getUser());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { broker.convertAndSend(
                    "/topic/users/" + participant.getUser().getId() + "/conversations", result); }
        });
        return result;
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
        List<ConversationUserResponse> people = participants.findAllByConversationId(conversation.getId()).stream()
                .map(ConversationParticipant::getUser).map(this::userResponse).toList();
        var membership = participants.findByConversationIdAndUserId(conversation.getId(), viewer.getId()).orElseThrow();
        long after = membership.getLastReadMessage() == null ? 0L : membership.getLastReadMessage().getId();
        ConversationMessageResponse last = messages.findTopByConversationIdOrderByIdDesc(conversation.getId()).map(this::messageResponse).orElse(null);
        return response(conversation, viewer, people, last, messages.countUnread(conversation.getId(), after, viewer.getId()));
    }
    private ConversationResponse response(Conversation conversation, User viewer, List<ConversationUserResponse> people,
            ConversationMessageResponse last, long unread) {
        String name = people.stream().filter(p -> !p.id().equals(viewer.getId())).map(ConversationUserResponse::displayName)
                .reduce((a, b) -> a + ", " + b).orElse("Direct message");
        return new ConversationResponse(conversation.getId(), conversation.getType(), people, name, last, unread,
                conversation.getCreatedAt(), conversation.getUpdatedAt());
    }
    private ConversationUserResponse userResponse(User u) { return new ConversationUserResponse(u.getId(), u.getDisplayName(), u.getEmail()); }
    private ConversationMessageResponse messageResponse(ConversationMessage m) {
        return new ConversationMessageResponse(m.getId(), m.getConversation().getId(), m.getSender().getId(),
                m.getSender().getDisplayName(), m.getContent(), m.getCreatedAt());
    }
    private record UserConversationUpdate(Long userId, ConversationResponse response) {}
    private void broadcastAfterCommit(ConversationMessageResponse message, List<UserConversationUpdate> updates) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                broker.convertAndSend("/topic/conversations/" + message.conversationId() + "/messages", message);
                updates.forEach(update -> broker.convertAndSend(
                        "/topic/users/" + update.userId() + "/conversations", update.response()));
            }
        });
    }
}
