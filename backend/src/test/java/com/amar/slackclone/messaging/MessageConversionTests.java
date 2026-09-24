package com.amar.slackclone.messaging;

import com.amar.slackclone.config.RabbitMQConfig;
import com.amar.slackclone.messaging.event.MessageCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.time.OffsetDateTime;
import java.util.List;
import com.amar.slackclone.message.dto.MessageResponse;
import static org.junit.jupiter.api.Assertions.*;

class MessageConversionTests {
    @Test void eventRoundTripsAsJson() {
        var converter = new RabbitMQConfig().rabbitMessageConverter(Jackson2ObjectMapperBuilder.json().build());
        var event = new MessageCreatedEvent(10L, new MessageResponse(1L, 2L, 3L, "Amar", "amar@example.com",
                "Hello \u010d \uD83D\uDC4B", OffsetDateTime.parse("2026-09-24T10:00:00Z"), OffsetDateTime.parse("2026-09-24T10:00:00Z"),
                null, List.of(), false, null, 0, List.of(), List.of()), null);
        var message = converter.toMessage(event, new MessageProperties());
        assertEquals("application/json", message.getMessageProperties().getContentType());
        assertEquals(event, converter.fromMessage(message));
        var consumer = new com.amar.slackclone.messaging.consumer.MessageEventConsumer(org.mockito.Mockito.mock(org.springframework.messaging.simp.SimpMessagingTemplate.class));
        assertDoesNotThrow(() -> consumer.receive((MessageCreatedEvent) converter.fromMessage(message)));
        message.getMessageProperties().getHeaders().clear();
        message.getMessageProperties().setInferredArgumentType(MessageCreatedEvent.class);
        assertEquals(event, converter.fromMessage(message));
    }
}
