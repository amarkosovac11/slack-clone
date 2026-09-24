package com.amar.slackclone.messaging;

import com.amar.slackclone.config.RabbitMQConfig;
import com.amar.slackclone.message.dto.*;
import com.amar.slackclone.messaging.consumer.MessageEventConsumer;
import com.amar.slackclone.messaging.event.MessageCreatedEvent;
import com.amar.slackclone.messaging.producer.MessageEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.OffsetDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@org.springframework.test.context.junit.jupiter.SpringJUnitConfig(org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class)
class MessageDeliveryTests {
    @org.springframework.beans.factory.annotation.Autowired
    private com.fasterxml.jackson.databind.ObjectMapper mapper;
    private final SimpMessagingTemplate broker = mock(SimpMessagingTemplate.class);
    private final MessageEventConsumer consumer = new MessageEventConsumer(broker);
    private static final String DESTINATION = "/topic/workspaces/10/channels/20/messages";

    @Test void producerUsesExistingExchangeAndRoutingKey() {
        var rabbit = mock(RabbitTemplate.class);
        var event = new MessageCreatedEvent(10L, message(30L, null), null);
        new MessageEventProducer(rabbit).send(event);
        verify(rabbit).convertAndSend(RabbitMQConfig.MESSAGE_EXCHANGE, RabbitMQConfig.MESSAGE_ROUTING_KEY, event);
        verifyNoMoreInteractions(rabbit);
    }

    @Test void jsonEventProducesOneFrontendCompatibleCreation() throws Exception {
        var converter = new RabbitMQConfig().rabbitMessageConverter(mapper);
        var event = new MessageCreatedEvent(10L, message(30L, null), null);
        consumer.receive((MessageCreatedEvent) converter.fromMessage(converter.toMessage(event, new MessageProperties())));
        var payload = org.mockito.ArgumentCaptor.forClass(ChannelMessageEvent.class);
        verify(broker).convertAndSend(eq(DESTINATION), payload.capture());
        verifyNoMoreInteractions(broker);
        var json = mapper.readTree(mapper.writeValueAsBytes(payload.getValue()));
        assertEquals("MESSAGE_CREATED", json.get("type").asText());
        assertTrue(json.get("threadRootMessageId").isNull());
        assertEquals(mapper.readTree(mapper.writeValueAsBytes(event.message())), json.get("message"));
        assertTrue(json.get("message").get("createdAt").isTextual());
    }

    @Test void replyDeliversCreationThenRootUpdate() {
        var event = new MessageCreatedEvent(10L, message(31L, 30L), message(30L, null));
        var converter = new RabbitMQConfig().rabbitMessageConverter(mapper);
        consumer.receive((MessageCreatedEvent) converter.fromMessage(converter.toMessage(event, new MessageProperties())));
        var order = inOrder(broker);
        order.verify(broker).convertAndSend(DESTINATION, new ChannelMessageEvent(
                ChannelMessageEventType.THREAD_REPLY_CREATED, event.message(), 30L));
        order.verify(broker).convertAndSend(DESTINATION, new ChannelMessageEvent(
                ChannelMessageEventType.THREAD_UPDATED, event.threadRoot(), 30L));
        verifyNoMoreInteractions(broker);
    }

    private MessageResponse message(Long id, Long rootId) {
        var time = OffsetDateTime.parse("2026-09-24T10:00:00Z");
        return new MessageResponse(id, 20L, 1L, "Amar", "amar@example.com", "Hello", time, time, null,
                List.of(new MentionResponse(2L, "User", "user")), false, rootId, 0, List.of(), List.of());
    }
}

