package com.amar.slackclone.message;import org.springframework.data.jpa.repository.*;import java.util.*;
public interface ChannelMessageReactionRepository extends JpaRepository<ChannelMessageReaction,Long>{@EntityGraph(attributePaths="user")List<ChannelMessageReaction> findAllByMessageId(Long id);Optional<ChannelMessageReaction> findByMessageIdAndUserIdAndEmoji(Long m,Long u,String e);}
