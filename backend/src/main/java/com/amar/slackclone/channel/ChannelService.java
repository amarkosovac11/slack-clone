package com.amar.slackclone.channel;

import com.amar.slackclone.channel.dto.ChannelResponse;
import com.amar.slackclone.channel.dto.ChannelMemberResponse;
import com.amar.slackclone.channel.dto.CreateChannelRequest;
import com.amar.slackclone.channel.dto.UpdateChannelRequest;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import com.amar.slackclone.workspace.Workspace;
import com.amar.slackclone.workspace.WorkspaceMember;
import com.amar.slackclone.workspace.WorkspaceMemberRepository;
import com.amar.slackclone.workspace.WorkspaceRepository;
import com.amar.slackclone.workspace.WorkspaceRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.time.OffsetDateTime;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final ChannelMemberRepository channelMemberRepository;

    public ChannelService(
            ChannelRepository channelRepository,
            WorkspaceRepository workspaceRepository,
            ChannelMemberRepository channelMemberRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository
    ) {
        this.channelRepository = channelRepository;
        this.workspaceRepository = workspaceRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChannelResponse createChannel(
            Long workspaceId,
            CreateChannelRequest request,
            String currentUserEmail
    ) {
        User currentUser = getCurrentUser(currentUserEmail);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() ->
                        new WorkspaceNotFoundException(workspaceId)
                );

        WorkspaceMember membership = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(
                        workspaceId,
                        currentUser.getId()
                )
                .orElseThrow(() ->
                        new WorkspaceAccessDeniedException(
                                "You are not a member of this workspace"
                        )
                );

        if (membership.getRole() != WorkspaceRole.OWNER
                && membership.getRole() != WorkspaceRole.ADMIN) {
            throw new WorkspaceAccessDeniedException(
                    "Only workspace owners and admins can create channels"
            );
        }

        String name = request.name().trim();
        String slug = generateUniqueSlug(workspaceId, name);

        Channel channel = new Channel();
        channel.setWorkspace(workspace);
        channel.setName(name);
        channel.setSlug(slug);
        channel.setDescription(normalizeDescription(request.description()));
        channel.setPrivateChannel(request.privateChannel());
        channel.setCreatedBy(currentUser);

        Channel savedChannel = channelRepository.save(channel);

        if (savedChannel.isPrivateChannel()) {
            ChannelMember channelMember = new ChannelMember();
            channelMember.setChannel(savedChannel);
            channelMember.setUser(currentUser);

            channelMemberRepository.save(channelMember);
        }

        return toResponse(savedChannel);
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> getChannels(
            Long workspaceId,
            String currentUserEmail
    ) {
        User currentUser = getCurrentUser(currentUserEmail);

        workspaceRepository.findById(workspaceId)
                .orElseThrow(() ->
                        new WorkspaceNotFoundException(workspaceId)
                );

        workspaceMemberRepository
                .findByWorkspaceIdAndUserId(
                        workspaceId,
                        currentUser.getId()
                )
                .orElseThrow(() ->
                        new WorkspaceAccessDeniedException(
                                "You are not a member of this workspace"
                        )
                );

        return channelRepository
                .findVisibleChannels(
                        workspaceId,
                        currentUser.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ChannelResponse updateChannel(Long workspaceId, Long channelId,
            UpdateChannelRequest request, String email) {
        requireManager(workspaceId, email);
        Channel channel = requireChannel(workspaceId, channelId);
        requireActive(channel);
        String name = request.name().trim();
        if (!name.equals(channel.getName())) {
            String slug = slugify(name);
            if (channelRepository.existsByWorkspaceIdAndSlugAndIdNot(workspaceId, slug, channelId)) {
                throw new ChannelConflictException("A channel with this name already exists in the workspace");
            }
            channel.setName(name);
            channel.setSlug(slug);
        }
        channel.setDescription(normalizeDescription(request.description()));
        return toResponse(channel);
    }

    @Transactional
    public ChannelResponse archiveChannel(Long workspaceId, Long channelId, String email) {
        requireManager(workspaceId, email);
        Channel channel = requireChannel(workspaceId, channelId);
        requireActive(channel);
        channel.setArchivedAt(OffsetDateTime.now());
        return toResponse(channel);
    }

    @Transactional
    public void deleteChannel(Long workspaceId, Long channelId, String email) {
        requireManager(workspaceId, email);
        channelRepository.delete(requireChannel(workspaceId, channelId));
    }

    private WorkspaceMember requireManager(Long workspaceId, String email) {
        User user = getCurrentUser(email);
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));
        WorkspaceMember membership = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() -> new WorkspaceAccessDeniedException("You are not a member of this workspace"));
        if (membership.getRole() != WorkspaceRole.OWNER && membership.getRole() != WorkspaceRole.ADMIN) {
            throw new WorkspaceAccessDeniedException("Only workspace owners and admins can manage channels");
        }
        return membership;
    }

    private Channel requireChannel(Long workspaceId, Long channelId) {
        return channelRepository.findByIdAndWorkspaceId(channelId, workspaceId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));
    }

    private void requireActive(Channel channel) {
        if (channel.isArchived()) throw new ChannelConflictException("Channel is archived");
    }

    @Transactional
    public ChannelResponse addMember(
            Long workspaceId,
            Long channelId,
            Long userId,
            String currentUserEmail
    ) {
        User currentUser = getCurrentUser(currentUserEmail);

        workspaceRepository.findById(workspaceId)
                .orElseThrow(() ->
                        new WorkspaceNotFoundException(workspaceId)
                );

        WorkspaceMember currentMembership = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(
                        workspaceId,
                        currentUser.getId()
                )
                .orElseThrow(() ->
                        new WorkspaceAccessDeniedException(
                                "You are not a member of this workspace"
                        )
                );

        if (currentMembership.getRole() != WorkspaceRole.OWNER
                && currentMembership.getRole() != WorkspaceRole.ADMIN) {
            throw new WorkspaceAccessDeniedException(
                    "Only workspace owners and admins can manage channel members"
            );
        }

        Channel channel = channelRepository
                .findByIdAndWorkspaceId(
                        channelId,
                        workspaceId
                )
                .orElseThrow(() ->
                        new ChannelNotFoundException(channelId)
                );

        if (!channel.isPrivateChannel()) {
            throw new ChannelMembershipConflictException(
                    "Members can only be managed for private channels"
            );
        }

        User userToAdd = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(userId)
                );

        workspaceMemberRepository
                .findByWorkspaceIdAndUserId(
                        workspaceId,
                        userToAdd.getId()
                )
                .orElseThrow(() ->
                        new WorkspaceAccessDeniedException(
                                "User is not a member of this workspace"
                        )
                );

        if (channelMemberRepository
                .existsByChannelIdAndUserId(
                        channelId,
                        userId
                )) {
            throw new ChannelMembershipConflictException(
                    "User is already a member of this channel"
            );
        }

        ChannelMember channelMember = new ChannelMember();
        channelMember.setChannel(channel);
        channelMember.setUser(userToAdd);

        channelMemberRepository.save(channelMember);

        return toResponse(channel);
    }

    @Transactional
    public void removeMember(
            Long workspaceId,
            Long channelId,
            Long userId,
            String currentUserEmail
    ) {
        User currentUser = getCurrentUser(currentUserEmail);

        workspaceRepository.findById(workspaceId)
                .orElseThrow(() ->
                        new WorkspaceNotFoundException(workspaceId)
                );

        WorkspaceMember currentMembership = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(
                        workspaceId,
                        currentUser.getId()
                )
                .orElseThrow(() ->
                        new WorkspaceAccessDeniedException(
                                "You are not a member of this workspace"
                        )
                );

        if (currentMembership.getRole() != WorkspaceRole.OWNER
                && currentMembership.getRole() != WorkspaceRole.ADMIN) {
            throw new WorkspaceAccessDeniedException(
                    "Only workspace owners and admins can manage channel members"
            );
        }

        Channel channel = channelRepository
                .findByIdAndWorkspaceId(
                        channelId,
                        workspaceId
                )
                .orElseThrow(() ->
                        new ChannelNotFoundException(channelId)
                );

        if (!channel.isPrivateChannel()) {
            throw new ChannelMembershipConflictException(
                    "Members can only be managed for private channels"
            );
        }

        ChannelMember membership = channelMemberRepository
                .findByChannelIdAndUserId(
                        channelId,
                        userId
                )
                .orElseThrow(() ->
                        new ChannelMembershipConflictException(
                                "User is not a member of this channel"
                        )
                );

        if (channel.getCreatedBy().getId().equals(userId)) {
            throw new ChannelMembershipConflictException(
                    "The channel creator cannot be removed"
            );
        }

        channelMemberRepository.delete(membership);
    }

    @Transactional(readOnly = true)
    public List<ChannelMemberResponse> getChannelMembers(
            Long workspaceId,
            Long channelId,
            String authenticatedEmail
    ) {
        User currentUser = getCurrentUser(authenticatedEmail);

        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));

        WorkspaceMember currentMembership = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, currentUser.getId())
                .orElseThrow(() -> new WorkspaceAccessDeniedException(
                        "You are not a member of this workspace"
                ));

        Channel channel = channelRepository
                .findByIdAndWorkspaceId(channelId, workspaceId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));

        if (currentMembership.getRole() != WorkspaceRole.OWNER
                && currentMembership.getRole() != WorkspaceRole.ADMIN) {
            throw new WorkspaceAccessDeniedException(
                    "Only workspace owners and admins can manage channel members"
            );
        }

        if (!channel.isPrivateChannel()) {
            throw new ChannelMembershipConflictException(
                    "Members can only be managed for private channels"
            );
        }

        return channelMemberRepository
                .findAllByChannelId(channelId)
                .stream()
                .map(this::toChannelMemberResponse)
                .toList();
    }

    private ChannelMemberResponse toChannelMemberResponse(
            ChannelMember membership
    ) {
        User user = membership.getUser();

        return new ChannelMemberResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                membership.getJoinedAt()
        );
    }

    private User getCurrentUser(String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(
                        AuthenticatedUserNotFoundException::new
                );
    }

    private String generateUniqueSlug(
            Long workspaceId,
            String name
    ) {
        String baseSlug = slugify(name);
        String slug = baseSlug;

        int suffix = 2;

        while (channelRepository
                .existsByWorkspaceIdAndSlug(
                        workspaceId,
                        slug
                )) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        return slug;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(
                value,
                Normalizer.Form.NFD
        );

        normalized = normalized.replaceAll("\\p{M}", "");

        return normalized
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String trimmed = description.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private ChannelResponse toResponse(Channel channel) {
        return new ChannelResponse(
                channel.getId(),
                channel.getWorkspace().getId(),
                channel.getName(),
                channel.getSlug(),
                channel.getDescription(),
                channel.isPrivateChannel(),
                channel.getCreatedBy().getId(),
                channel.getCreatedAt(),
                channel.getUpdatedAt(),
                channel.getArchivedAt()
        );
    }
}
