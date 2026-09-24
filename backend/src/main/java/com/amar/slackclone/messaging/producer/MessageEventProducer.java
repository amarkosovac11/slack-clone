package com.amar.slackclone.messaging.producer;

import com.amar.slackclone.config.RabbitMQConfig;
import com.amar.slackclone.messaging.event.MessageCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public MessageEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(MessageCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MESSAGE_EXCHANGE,
                RabbitMQConfig.MESSAGE_ROUTING_KEY,
                event);
    }
}
