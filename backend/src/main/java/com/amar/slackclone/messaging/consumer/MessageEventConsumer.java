package com.amar.slackclone.messaging.consumer;

import com.amar.slackclone.config.RabbitMQConfig;
import com.amar.slackclone.messaging.event.MessageCreatedEvent;
import com.amar.slackclone.message.dto.ChannelMessageEvent;
import com.amar.slackclone.message.dto.ChannelMessageEventType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MessageEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(MessageEventConsumer.class);
    private final SimpMessagingTemplate messagingTemplate;

    public MessageEventConsumer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.MESSAGE_QUEUE)
    public void receive(MessageCreatedEvent event) {
        String destination = "/topic/workspaces/%d/channels/%d/messages"
                .formatted(event.workspaceId(), event.message().channelId());
        Long rootId = event.message().threadRootMessageId();
        messagingTemplate.convertAndSend(destination, new ChannelMessageEvent(
                rootId == null ? ChannelMessageEventType.MESSAGE_CREATED : ChannelMessageEventType.THREAD_REPLY_CREATED,
                event.message(), rootId));
        if (event.threadRoot() != null) {
            messagingTemplate.convertAndSend(destination, new ChannelMessageEvent(
                    ChannelMessageEventType.THREAD_UPDATED, event.threadRoot(), rootId));
        }
        log.debug("Received message-created event for message {} in channel {}",
                event.message().id(), event.message().channelId());
    }
}
