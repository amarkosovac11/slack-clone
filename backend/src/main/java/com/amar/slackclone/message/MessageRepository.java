package com.amar.slackclone.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findAllByChannelIdAndDeletedAtIsNullOrderByCreatedAtAsc(
        Long channelId
    );
}