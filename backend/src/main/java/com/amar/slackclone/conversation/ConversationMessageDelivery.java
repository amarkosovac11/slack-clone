package com.amar.slackclone.conversation;

import com.amar.slackclone.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="conversation_message_deliveries", uniqueConstraints=@UniqueConstraint(columnNames={"message_id","user_id"}))
public class ConversationMessageDelivery {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="message_id") private ConversationMessage message;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
    @Column(name="delivered_at",nullable=false) private OffsetDateTime deliveredAt;
    @PrePersist void create(){if(deliveredAt==null)deliveredAt=OffsetDateTime.now();}
    public ConversationMessage getMessage(){return message;} public void setMessage(ConversationMessage value){message=value;}
    public User getUser(){return user;} public void setUser(User value){user=value;}
    public OffsetDateTime getDeliveredAt(){return deliveredAt;}
}
