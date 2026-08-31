package com.amar.slackclone.user;

import com.amar.slackclone.attachment.FileStorageService;
import com.amar.slackclone.auth.InvalidCredentialsException;
import com.amar.slackclone.user.dto.*;
import org.springframework.core.io.Resource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.amar.slackclone.presence.PresenceService;
import com.amar.slackclone.workspace.WorkspaceMemberRepository;

import java.io.IOException;
import java.time.Instant;

@Service
public class UserProfileService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final FileStorageService storage;
    private final SimpMessagingTemplate messaging;
    private final PresenceService presence;
    private final WorkspaceMemberRepository members;

    public UserProfileService(UserRepository users, PasswordEncoder passwords,
            FileStorageService storage, SimpMessagingTemplate messaging, PresenceService presence,
            WorkspaceMemberRepository members) {
        this.users=users; this.passwords=passwords; this.storage=storage; this.messaging=messaging; this.presence=presence; this.members=members;
    }

    @Transactional(readOnly=true)
    public CurrentUserProfileResponse me(String email) { User u=user(email); return response(u, presence.status(u.getId()).name()); }

    @Transactional(readOnly=true)
    public UserSummaryResponse summary(String email, Long userId) {
        User viewer=user(email);
        if(!viewer.getId().equals(userId)&&!members.shareWorkspace(viewer.getId(),userId)) throw new SecurityException("You cannot view this user profile");
        User target=users.findById(userId).orElseThrow(()->new IllegalArgumentException("User not found"));boolean active=activeStatus(target);
        return new UserSummaryResponse(target.getId(),target.getDisplayName(),target.getTitle(),avatarUrl(target),active?target.getCustomStatusText():null,active?target.getCustomStatusEmoji():null,active?target.getCustomStatusExpiresAt():null,presence.status(target.getId()).name(),target.getLastSeenAt());
    }

    @Transactional
    public CurrentUserProfileResponse update(String email, UpdateProfileRequest request) {
        User user=user(email); user.setDisplayName(request.displayName().trim()); user.setTitle(trimToNull(request.title()));
        user.setUpdatedAt(Instant.now()); publish(user,"PROFILE_UPDATED"); return response(user,presence.status(user.getId()).name());
    }

    @Transactional
    public CurrentUserProfileResponse status(String email, UpdateStatusRequest request) {
        if(request.expiresAt()!=null&&!request.expiresAt().isAfter(Instant.now())) throw new IllegalArgumentException("Status expiration must be in the future");
        User user=user(email); user.setCustomStatusText(trimToNull(request.text())); user.setCustomStatusEmoji(trimToNull(request.emoji()));
        user.setCustomStatusExpiresAt(request.expiresAt()); user.setUpdatedAt(Instant.now()); publish(user,"STATUS_UPDATED"); return response(user,presence.status(user.getId()).name());
    }

    @Transactional
    public CurrentUserProfileResponse clearStatus(String email) { return status(email,new UpdateStatusRequest(null,null,null)); }

    @Transactional
    public CurrentUserProfileResponse avatar(String email, MultipartFile file) throws IOException {
        User user=user(email); String old=user.getAvatarKey(); String key=storage.storeAvatar(file); user.setAvatarKey(key); user.setUpdatedAt(Instant.now());
        if(old!=null) storage.deleteAvatar(old); publish(user,"PROFILE_UPDATED"); return response(user,presence.status(user.getId()).name());
    }

    @Transactional
    public CurrentUserProfileResponse removeAvatar(String email) throws IOException {
        User user=user(email); String old=user.getAvatarKey(); user.setAvatarKey(null); user.setUpdatedAt(Instant.now());
        storage.deleteAvatar(old); publish(user,"PROFILE_UPDATED"); return response(user,presence.status(user.getId()).name());
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user=user(email); if(!passwords.matches(request.currentPassword(),user.getPasswordHash())) throw new InvalidCredentialsException();
        user.setPasswordHash(passwords.encode(request.newPassword())); user.setUpdatedAt(Instant.now());
    }

    public Resource avatarResource(String key) { return storage.loadAvatar(key); }
    private User user(String email){return users.findByEmailIgnoreCase(email).orElseThrow(InvalidCredentialsException::new);}
    private String trimToNull(String value){if(value==null)return null;String v=value.trim();return v.isEmpty()?null:v;}
    private String avatarUrl(User u){return u.getAvatarKey()==null?null:"/api/users/avatars/"+u.getAvatarKey();}
    private boolean activeStatus(User u){return u.getCustomStatusExpiresAt()==null||u.getCustomStatusExpiresAt().isAfter(Instant.now());}
    private CurrentUserProfileResponse response(User u,String presence){boolean active=activeStatus(u);return new CurrentUserProfileResponse(u.getId(),u.getEmail(),u.getDisplayName(),u.getTitle(),avatarUrl(u),active?u.getCustomStatusText():null,active?u.getCustomStatusEmoji():null,active?u.getCustomStatusExpiresAt():null,presence,u.getLastSeenAt(),u.getCreatedAt());}
    private void publish(User u,String type){boolean active=activeStatus(u);var event=new UserProfileEvent(type,u.getId(),u.getDisplayName(),u.getTitle(),avatarUrl(u),active?u.getCustomStatusText():null,active?u.getCustomStatusEmoji():null,active?u.getCustomStatusExpiresAt():null,null,u.getLastSeenAt());messaging.convertAndSend("/topic/users/"+u.getId()+"/profile-events",event);members.findMessageableUsers(u.getId()).forEach(viewer->messaging.convertAndSend("/topic/users/"+viewer.getId()+"/profile-events",event));}
}
