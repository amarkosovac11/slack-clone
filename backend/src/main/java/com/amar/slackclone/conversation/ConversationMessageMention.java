package com.amar.slackclone.conversation;
import com.amar.slackclone.user.User; import jakarta.persistence.*;
@Entity @Table(name="conversation_message_mentions") @IdClass(ConversationMessageMentionId.class)
public class ConversationMessageMention { @Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="message_id") private ConversationMessage message;
 @Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user; public ConversationMessageMention(){} public ConversationMessageMention(ConversationMessage m,User u){message=m;user=u;}
 public User getUser(){return user;} }
