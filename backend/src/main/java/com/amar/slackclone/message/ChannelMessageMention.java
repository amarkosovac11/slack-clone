package com.amar.slackclone.message;
import com.amar.slackclone.user.User;
import jakarta.persistence.*;
@Entity @Table(name="channel_message_mentions") @IdClass(ChannelMessageMentionId.class)
public class ChannelMessageMention {
 @Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="message_id") private Message message;
 @Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
 public ChannelMessageMention() {} public ChannelMessageMention(Message m, User u){message=m;user=u;}
 public Message getMessage(){return message;} public User getUser(){return user;}
}
