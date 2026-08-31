package com.amar.slackclone.presence;

import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import com.amar.slackclone.user.dto.UserProfileEvent;
import com.amar.slackclone.workspace.WorkspaceMemberRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class PresenceService {
    static final Duration AWAY_AFTER=Duration.ofMinutes(5);
    private final ConcurrentMap<String,Session> sessions=new ConcurrentHashMap<>();
    private final ConcurrentMap<Long,UserPresenceStatus> announced=new ConcurrentHashMap<>();
    private final UserRepository users; private final WorkspaceMemberRepository members; private final SimpMessagingTemplate messaging;
    public PresenceService(UserRepository users,WorkspaceMemberRepository members,SimpMessagingTemplate messaging){this.users=users;this.members=members;this.messaging=messaging;}
    public void connect(String sessionId,String email){User user=user(email);UserPresenceStatus before=status(user.getId());sessions.put(sessionId,new Session(user.getId(),Instant.now()));if(before==UserPresenceStatus.OFFLINE){announced.put(user.getId(),UserPresenceStatus.ONLINE);publish(user,UserPresenceStatus.ONLINE,null);}}
    @Transactional public void disconnect(String sessionId){Session removed=sessions.remove(sessionId);if(removed==null)return;if(sessionCount(removed.userId())==0){announced.put(removed.userId(),UserPresenceStatus.OFFLINE);users.findById(removed.userId()).ifPresent(user->{Instant now=Instant.now();user.setLastSeenAt(now);user.setUpdatedAt(now);publish(user,UserPresenceStatus.OFFLINE,now);});}}
    public void activity(String sessionId,String email){User user=user(email);Session old=sessions.get(sessionId);if(old==null||!old.userId().equals(user.getId()))return;UserPresenceStatus before=status(user.getId());sessions.put(sessionId,new Session(user.getId(),Instant.now()));if(before==UserPresenceStatus.AWAY){announced.put(user.getId(),UserPresenceStatus.ONLINE);publish(user,UserPresenceStatus.ONLINE,null);}}
    public UserPresenceStatus status(Long userId){Instant latest=sessions.values().stream().filter(s->s.userId().equals(userId)).map(Session::activeAt).max(Instant::compareTo).orElse(null);if(latest==null)return UserPresenceStatus.OFFLINE;return latest.isBefore(Instant.now().minus(AWAY_AFTER))?UserPresenceStatus.AWAY:UserPresenceStatus.ONLINE;}
    public int sessionCount(Long userId){return(int)sessions.values().stream().filter(s->s.userId().equals(userId)).count();}
    @Scheduled(fixedDelay=30000) public void detectAway(){Set<Long> ids=ConcurrentHashMap.newKeySet();sessions.values().forEach(s->ids.add(s.userId()));ids.forEach(id->{if(status(id)==UserPresenceStatus.AWAY&&announced.get(id)!=UserPresenceStatus.AWAY){announced.put(id,UserPresenceStatus.AWAY);users.findById(id).ifPresent(u->publish(u,UserPresenceStatus.AWAY,null));}});}
    private User user(String email){return users.findByEmailIgnoreCase(email).orElseThrow(()->new IllegalArgumentException("Unknown presence user"));}
    private void publish(User user,UserPresenceStatus status,Instant lastSeen){UserProfileEvent event=new UserProfileEvent("PRESENCE_UPDATED",user.getId(),user.getDisplayName(),user.getTitle(),user.getAvatarKey()==null?null:"/api/users/avatars/"+user.getAvatarKey(),null,null,null,status.name(),lastSeen);messaging.convertAndSend("/topic/users/"+user.getId()+"/profile-events",event);members.findMessageableUsers(user.getId()).forEach(viewer->messaging.convertAndSend("/topic/users/"+viewer.getId()+"/profile-events",event));}
    private record Session(Long userId,Instant activeAt){}
}
