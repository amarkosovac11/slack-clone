package com.amar.slackclone.channel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelMemberRepository
        extends JpaRepository<ChannelMember, Long> {

    boolean existsByChannelIdAndUserId(
            Long channelId,
            Long userId
    );

    Optional<ChannelMember> findByChannelIdAndUserId(
            Long channelId,
            Long userId
    );

    List<ChannelMember> findAllByChannelId(
            Long channelId
    );

    List<ChannelMember> findAllByUserId(
            Long userId
    );
}