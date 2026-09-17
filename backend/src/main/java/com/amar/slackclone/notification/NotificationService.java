package com.amar.slackclone.notification;

import com.amar.slackclone.notification.dto.*;
import com.amar.slackclone.user.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final SimpMessagingTemplate messaging;
    public NotificationService(NotificationRepository notifications, UserRepository users, SimpMessagingTemplate messaging) {
        this.notifications=notifications; this.users=users; this.messaging=messaging;
    }

    @Transactional
    public NotificationResponse create(Long recipientId, Long actorId, NotificationType type, String text, Context context) {
        if (recipientId == null || recipientId.equals(actorId)) return null;
        Notification n=new Notification(); n.setRecipient(users.findById(recipientId).orElseThrow());
        if(actorId!=null)n.setActor(users.findById(actorId).orElseThrow()); n.setType(type); n.setText(text);
        if(context!=null){n.setWorkspaceId(context.workspaceId());n.setChannelId(context.channelId());n.setConversationId(context.conversationId());n.setChannelMessageId(context.channelMessageId());n.setConversationMessageId(context.conversationMessageId());}
        n=notifications.saveAndFlush(n); NotificationResponse response=response(n); publishAfterCommit(recipientId,"CREATED",response); return response;
    }

    @Transactional(readOnly=true)
    public List<NotificationResponse> list(String email,int requestedLimit){User u=user(email);int limit=Math.max(1,Math.min(requestedLimit,100));return notifications.findAllByRecipientIdOrderByCreatedAtDesc(u.getId(),PageRequest.of(0,limit)).stream().map(this::response).toList();}
    @Transactional(readOnly=true) public long unreadCount(String email){return notifications.countByRecipientIdAndReadAtIsNull(user(email).getId());}
    @Transactional public NotificationResponse markRead(Long id,String email){User u=user(email);Notification n=notifications.findByIdAndRecipientId(id,u.getId()).orElseThrow(()->new SecurityException("Notification not found"));if(n.getReadAt()==null)n.setReadAt(OffsetDateTime.now());NotificationResponse r=response(n);publishAfterCommit(u.getId(),"READ",r);return r;}
    @Transactional public long markAllRead(String email){User u=user(email);notifications.markAllRead(u.getId(),OffsetDateTime.now());long count=0;publishCountAfterCommit(u.getId(),count);return count;}
    private User user(String email){return users.findByEmailIgnoreCase(email).orElseThrow();}
    private NotificationResponse response(Notification n){User a=n.getActor();return new NotificationResponse(n.getId(),n.getType(),a==null?null:a.getId(),a==null?null:a.getDisplayName(),a==null||a.getAvatarKey()==null?null:"/api/users/avatars/"+a.getAvatarKey(),n.getWorkspaceId(),n.getChannelId(),n.getConversationId(),n.getChannelMessageId(),n.getConversationMessageId(),n.getText(),n.getCreatedAt(),n.getReadAt());}
    private void publishAfterCommit(Long userId,String type,NotificationResponse response){afterCommit(()->messaging.convertAndSend("/topic/users/"+userId+"/notifications",new NotificationEvent(type,response,notifications.countByRecipientIdAndReadAtIsNull(userId))));}
    private void publishCountAfterCommit(Long userId,long count){afterCommit(()->messaging.convertAndSend("/topic/users/"+userId+"/notifications",new NotificationEvent("COUNT_UPDATED",null,count)));}
    private void afterCommit(Runnable task){if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){task.run();}});else task.run();}
    public record Context(Long workspaceId,Long channelId,Long conversationId,Long channelMessageId,Long conversationMessageId){}
}
