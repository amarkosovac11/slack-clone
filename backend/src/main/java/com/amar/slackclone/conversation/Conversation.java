package com.amar.slackclone.conversation;

import com.amar.slackclone.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "conversations")
public class Conversation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ConversationType type;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by") private User createdBy;
    @Column(name = "direct_key", unique = true, length = 80) private String directKey;
    @Column(name = "custom_name", length = 100) private String customName;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist void create() { var now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; if (updatedAt == null) updatedAt = now; }
    public Long getId() { return id; }
    public ConversationType getType() { return type; }
    public void setType(ConversationType type) { this.type = type; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public String getDirectKey() { return directKey; }
    public void setDirectKey(String directKey) { this.directKey = directKey; }
    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void touch() { updatedAt = OffsetDateTime.now(); }
}
