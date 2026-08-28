package com.amar.slackclone.conversation;

import com.amar.slackclone.channel.AuthenticatedUserNotFoundException;
import com.amar.slackclone.user.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationAccessService {
    private final ConversationRepository conversations;
    private final ConversationParticipantRepository participants;
    private final UserRepository users;
    public ConversationAccessService(ConversationRepository conversations,
            ConversationParticipantRepository participants, UserRepository users) {
        this.conversations = conversations; this.participants = participants; this.users = users;
    }
    @Transactional(readOnly = true)
    public ConversationParticipant requireParticipant(Long conversationId, String email) {
        User user = requireUser(email);
        conversations.findById(conversationId).orElseThrow(() -> new ConversationNotFoundException(conversationId));
        return participants.findByConversationIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new ConversationAccessDeniedException("You are not a participant in this conversation"));
    }
    public User requireUser(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(AuthenticatedUserNotFoundException::new);
    }
}
