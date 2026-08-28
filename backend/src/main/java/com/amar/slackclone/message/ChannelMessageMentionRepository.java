package com.amar.slackclone.message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ChannelMessageMentionRepository extends JpaRepository<ChannelMessageMention,ChannelMessageMentionId>{
 List<ChannelMessageMention> findAllByMessageId(Long messageId); void deleteAllByMessageId(Long messageId);
}
