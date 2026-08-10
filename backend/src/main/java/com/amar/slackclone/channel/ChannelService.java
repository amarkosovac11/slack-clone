package com.amar.slackclone.channel;

import com.amar.slackclone.channel.dto.ChannelResponse;
import com.amar.slackclone.channel.dto.CreateChannelRequest;
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

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public ChannelService(
            ChannelRepository channelRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository) {
        this.channelRepository = channelRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChannelResponse createChannel(
            Long workspaceId,
            CreateChannelRequest request,
            String currentUserEmail) {
        User currentUser = getCurrentUser(currentUserEmail);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));

        WorkspaceMember membership = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, currentUser.getId())
                .orElseThrow(() -> new WorkspaceAccessDeniedException(
                        "You are not a member of this workspace"));

        if (membership.getRole() != WorkspaceRole.OWNER
                && membership.getRole() != WorkspaceRole.ADMIN) {
            throw new WorkspaceAccessDeniedException(
                    "Only workspace owners and admins can create channels");
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
            .findAllByWorkspaceIdOrderByCreatedAtAsc(workspaceId)
            .stream()
            .map(this::toResponse)
            .toList();
}

    private User getCurrentUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(AuthenticatedUserNotFoundException::new);
    }

    private String generateUniqueSlug(Long workspaceId, String name) {
        String baseSlug = slugify(name);
        String slug = baseSlug;

        int suffix = 2;

        while (channelRepository.existsByWorkspaceIdAndSlug(workspaceId, slug)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        return slug;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(
                value,
                Normalizer.Form.NFD);

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
                channel.getUpdatedAt());
    }
}