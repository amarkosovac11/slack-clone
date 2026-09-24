package com.amar.slackclone.config;

import org.springframework.amqp.core.Queue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.core.DirectExchange;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;

@Configuration
public class RabbitMQConfig {

    public static final String MESSAGE_QUEUE = "message.queue";
    public static final String MESSAGE_EXCHANGE = "message.exchange";
    public static final String MESSAGE_ROUTING_KEY = "message.created";

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper, "com.amar.slackclone.messaging.event");
    }

    @Bean
    public Queue messageQueue() {
        return new Queue(MESSAGE_QUEUE, true);
    }

    @Bean
    public DirectExchange messageExchange() {
        return new DirectExchange(MESSAGE_EXCHANGE);
    }

    @Bean
public Binding messageBinding(
        Queue messageQueue,
        DirectExchange messageExchange
) {
    return BindingBuilder
            .bind(messageQueue)
            .to(messageExchange)
            .with(MESSAGE_ROUTING_KEY);
}
}
