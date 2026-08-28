package com.amar.slackclone.conversation;

import com.amar.slackclone.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "conversation_messages")
public class ConversationMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "conversation_id") private Conversation conversation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sender_id") private User sender;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "deleted_at") private OffsetDateTime deletedAt;
    @PrePersist void create() { var now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; if (updatedAt == null) updatedAt = now; }
    public Long getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void edit(String content) { this.content = content; this.updatedAt = OffsetDateTime.now(); }
    public void softDelete() { this.content = null; this.deletedAt = OffsetDateTime.now(); this.updatedAt = this.deletedAt; }
}
