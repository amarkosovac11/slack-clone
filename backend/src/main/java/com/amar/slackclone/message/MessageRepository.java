package com.amar.slackclone.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findAllByChannelIdOrderByCreatedAtAsc(
        Long channelId
    );

    Optional<Message> findByIdAndChannelId(Long id, Long channelId);
    List<Message> findAllByThreadRootMessageIdOrderByCreatedAtAsc(Long id);
    long countByThreadRootMessageId(Long id);
    @Query("select m from Message m where m.channel.id in :ids and m.deletedAt is null and lower(m.content) like lower(concat('%',:q,'%')) order by m.createdAt desc")List<Message> searchAccessible(@Param("ids")Collection<Long> ids,@Param("q")String q,org.springframework.data.domain.Pageable p);
}
