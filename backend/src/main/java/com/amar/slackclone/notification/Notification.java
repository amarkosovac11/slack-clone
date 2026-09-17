package com.amar.slackclone.notification;

import com.amar.slackclone.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recipient_id") private User recipient;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_id") private User actor;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private NotificationType type;
    @Column(name = "workspace_id") private Long workspaceId;
    @Column(name = "channel_id") private Long channelId;
    @Column(name = "conversation_id") private Long conversationId;
    @Column(name = "channel_message_id") private Long channelMessageId;
    @Column(name = "conversation_message_id") private Long conversationMessageId;
    @Column(nullable = false, length = 500) private String text;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "read_at") private OffsetDateTime readAt;
    @PrePersist void create() { if (createdAt == null) createdAt = OffsetDateTime.now(); }
    public Long getId(){return id;} public User getRecipient(){return recipient;} public void setRecipient(User v){recipient=v;}
    public User getActor(){return actor;} public void setActor(User v){actor=v;} public NotificationType getType(){return type;}
    public void setType(NotificationType v){type=v;} public Long getWorkspaceId(){return workspaceId;} public void setWorkspaceId(Long v){workspaceId=v;}
    public Long getChannelId(){return channelId;} public void setChannelId(Long v){channelId=v;} public Long getConversationId(){return conversationId;}
    public void setConversationId(Long v){conversationId=v;} public Long getChannelMessageId(){return channelMessageId;} public void setChannelMessageId(Long v){channelMessageId=v;}
    public Long getConversationMessageId(){return conversationMessageId;} public void setConversationMessageId(Long v){conversationMessageId=v;}
    public String getText(){return text;} public void setText(String v){text=v;} public OffsetDateTime getCreatedAt(){return createdAt;}
    public OffsetDateTime getReadAt(){return readAt;} public void setReadAt(OffsetDateTime v){readAt=v;}
}
