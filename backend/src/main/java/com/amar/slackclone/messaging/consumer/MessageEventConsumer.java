package com.amar.slackclone.messaging.consumer;

import com.amar.slackclone.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MessageEventConsumer {

    @RabbitListener(queues = RabbitMQConfig.MESSAGE_QUEUE)
    public void receive(String message) {
        System.out.println("Primljena RabbitMQ poruka: " + message);
    }
}