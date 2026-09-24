package com.amar.slackclone.messaging.consumer;

import com.amar.slackclone.config.RabbitMQConfig;
import com.amar.slackclone.messaging.event.MessageCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MessageEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(MessageEventConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.MESSAGE_QUEUE)
    public void receive(MessageCreatedEvent event) {
        log.debug("Received message-created event for message {} in channel {}",
                event.message().id(), event.message().channelId());
    }
}
