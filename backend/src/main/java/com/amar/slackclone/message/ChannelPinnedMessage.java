package com.amar.slackclone.message;
import com.amar.slackclone.channel.Channel; import com.amar.slackclone.user.User; import jakarta.persistence.*; import java.time.OffsetDateTime;
@Entity @Table(name="channel_pinned_messages")
public class ChannelPinnedMessage { @Id @Column(name="message_id") private Long messageId; @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="message_id",insertable=false,updatable=false) private Message message;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="channel_id") private Channel channel; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="pinned_by") private User pinnedBy;
 @Column(name="pinned_at") private OffsetDateTime pinnedAt; @PrePersist void create(){if(pinnedAt==null)pinnedAt=OffsetDateTime.now();}
 public Message getMessage(){return message;} public void setMessage(Message v){message=v;messageId=v.getId();} public Channel getChannel(){return channel;} public void setChannel(Channel v){channel=v;}
 public User getPinnedBy(){return pinnedBy;} public void setPinnedBy(User v){pinnedBy=v;} public OffsetDateTime getPinnedAt(){return pinnedAt;}}
