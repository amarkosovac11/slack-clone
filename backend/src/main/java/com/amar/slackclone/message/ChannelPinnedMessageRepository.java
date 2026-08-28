package com.amar.slackclone.message;
import org.springframework.data.jpa.repository.*; import java.util.*;
public interface ChannelPinnedMessageRepository extends JpaRepository<ChannelPinnedMessage,Long>{
 @EntityGraph(attributePaths={"message","message.sender","pinnedBy"}) List<ChannelPinnedMessage> findAllByChannelIdOrderByPinnedAtDesc(Long channelId);
 Optional<ChannelPinnedMessage> findByMessageIdAndChannelId(Long messageId,Long channelId); void deleteByMessageId(Long messageId);
}
