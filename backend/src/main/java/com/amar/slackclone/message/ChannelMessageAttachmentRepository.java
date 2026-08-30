package com.amar.slackclone.message;import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface ChannelMessageAttachmentRepository extends JpaRepository<ChannelMessageAttachment,Long>{List<ChannelMessageAttachment> findAllByMessageId(Long id);}
