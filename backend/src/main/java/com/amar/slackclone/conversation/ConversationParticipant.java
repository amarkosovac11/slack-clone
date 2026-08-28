package com.amar.slackclone.conversation;

import com.amar.slackclone.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "conversation_participants", uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}))
public class ConversationParticipant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "conversation_id") private Conversation conversation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "joined_at", nullable = false) private OffsetDateTime joinedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "last_read_message_id") private ConversationMessage lastReadMessage;
    @Column(name = "hidden_at") private OffsetDateTime hiddenAt;
    @PrePersist void create() { if (joinedAt == null) joinedAt = OffsetDateTime.now(); }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public ConversationMessage getLastReadMessage() { return lastReadMessage; }
    public void setLastReadMessage(ConversationMessage message) { lastReadMessage = message; }
    public OffsetDateTime getHiddenAt() { return hiddenAt; }
    public void setHiddenAt(OffsetDateTime hiddenAt) { this.hiddenAt = hiddenAt; }
}
