package com.amar.slackclone.messaging;

import com.amar.slackclone.config.RabbitMQConfig;
import com.amar.slackclone.messaging.event.MessageCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class MessageConversionTests {
    @Test void eventRoundTripsAsJson() {
        var converter = new RabbitMQConfig().rabbitMessageConverter(Jackson2ObjectMapperBuilder.json().build());
        var event = new MessageCreatedEvent(1L, 2L, 3L, "Hello č 👋", Instant.parse("2026-09-24T10:00:00Z"));
        var message = converter.toMessage(event, new MessageProperties());
        assertEquals("application/json", message.getMessageProperties().getContentType());
        assertEquals(event, converter.fromMessage(message));
        message.getMessageProperties().getHeaders().clear();
        message.getMessageProperties().setInferredArgumentType(MessageCreatedEvent.class);
        assertEquals(event, converter.fromMessage(message));
    }
}
