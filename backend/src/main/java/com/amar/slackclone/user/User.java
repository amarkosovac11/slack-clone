package com.amar.slackclone.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 120)
    private String title;

    @Column(name = "avatar_key", length = 100)
    private String avatarKey;

    @Column(name = "custom_status_text", length = 100)
    private String customStatusText;

    @Column(name = "custom_status_emoji", length = 32)
    private String customStatusEmoji;

    @Column(name = "custom_status_expires_at")
    private Instant customStatusExpiresAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User() {
    }

    public User(
            String email,
            String passwordHash,
            String displayName,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAvatarKey() { return avatarKey; }
    public void setAvatarKey(String avatarKey) { this.avatarKey = avatarKey; }
    public String getCustomStatusText() { return customStatusText; }
    public void setCustomStatusText(String value) { this.customStatusText = value; }
    public String getCustomStatusEmoji() { return customStatusEmoji; }
    public void setCustomStatusEmoji(String value) { this.customStatusEmoji = value; }
    public Instant getCustomStatusExpiresAt() { return customStatusExpiresAt; }
    public void setCustomStatusExpiresAt(Instant value) { this.customStatusExpiresAt = value; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant value) { this.lastSeenAt = value; }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
