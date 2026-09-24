package com.amar.slackclone.messaging;

import com.amar.slackclone.config.RabbitMQConfig;
import com.amar.slackclone.message.dto.*;
import com.amar.slackclone.messaging.consumer.MessageEventConsumer;
import com.amar.slackclone.messaging.event.MessageCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.OffsetDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RabbitWiringTests {
    @Test void bootWiresJsonConverterIntoTemplateAndAnnotatedListener() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, RabbitAutoConfiguration.class))
                .withUserConfiguration(RabbitMQConfig.class, MessageEventConsumer.class)
                .withBean(SimpMessagingTemplate.class, () -> mock(SimpMessagingTemplate.class))
                .withPropertyValues("spring.rabbitmq.listener.simple.auto-startup=false")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    var converter = context.getBean(Jackson2JsonMessageConverter.class);
                    assertSame(converter, context.getBean(RabbitTemplate.class).getMessageConverter());
                    var containers = context.getBean(RabbitListenerEndpointRegistry.class).getListenerContainers();
                    assertEquals(1, containers.size());
                    var listener = (ChannelAwareMessageListener) containers.iterator().next().getMessageListener();
                    var time = OffsetDateTime.parse("2026-09-24T10:00:00Z");
                    var response = new MessageResponse(30L, 20L, 1L, "Amar", "amar@example.com", "hello",
                            time, time, null, List.of(), false, null, 0, List.of(), List.of());
                    var message = converter.toMessage(new MessageCreatedEvent(10L, response, null), new MessageProperties());
                    // Exercise the actual @RabbitListener adapter, including inferred argument conversion.
                    message.getMessageProperties().getHeaders().clear();
                    listener.onMessage(message, null);
                    var broker = context.getBean(SimpMessagingTemplate.class);
                    verify(broker).convertAndSend("/topic/workspaces/10/channels/20/messages",
                            new ChannelMessageEvent(ChannelMessageEventType.MESSAGE_CREATED, response, null));
                    verifyNoMoreInteractions(broker);
                });
    }
}
