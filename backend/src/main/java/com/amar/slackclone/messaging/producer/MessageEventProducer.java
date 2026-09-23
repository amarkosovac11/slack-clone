package com.amar.slackclone.messaging.producer;

import com.amar.slackclone.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class MessageEventProducer {

    private final RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void testSend() {
        send("RabbitMQ radi!");
    }

    public MessageEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MESSAGE_EXCHANGE,
                RabbitMQConfig.MESSAGE_ROUTING_KEY,
                message);
    }
}