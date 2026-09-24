# Channel message events

Channel messages and replies enter through the existing REST controller. `MessageService`
checks access, saves the message and related metadata synchronously, and captures the
existing `MessageResponse` DTO. Only a successful transaction commit triggers
`MessageEventProducer.send`.

`MessageCreatedEvent` contains the workspace ID, saved message DTO, and (for replies)
the updated thread-root DTO. The latter preserves the existing reply-count update
without loading entities in the RabbitMQ listener.

Spring Boot's ObjectMapper and the central `Jackson2JsonMessageConverter` bean handle
JSON, including Java time values. Boot configures the producer template and listener
factory with that converter. Events use `message.exchange`, routing key
`message.created`, and durable `message.queue`.

`MessageEventConsumer` sends the existing `ChannelMessageEvent` to
`/topic/workspaces/{workspaceId}/channels/{channelId}/messages`. Normal creations emit
`MESSAGE_CREATED`; replies emit `THREAD_REPLY_CREATED` followed by `THREAD_UPDATED`.
MessageService no longer broadcasts these events directly. Updates, deletions,
reactions, attachments, pins, notifications, and direct conversations retain their
existing paths. Existing STOMP subscription authorization remains in place.

## Delivery limits

- This is an after-commit publish, not a transactional outbox. A broker outage or
  process failure after the database commit can leave a saved message without its
  event. A publish exception can reach the HTTP caller after the save has committed.
- The two competing creation broadcast paths are removed. RabbitMQ redelivery can
  still repeat a WebSocket event; this does not provide exactly-once delivery.
- Creation events are asynchronous while edits and other mutations still broadcast
  directly. Concurrent mutations can overtake a delayed creation event.
- The existing simple STOMP broker is local to one application instance. A shared
  RabbitMQ queue distributes work among consumers, so this design assumes one
  backend instance; it does not fan out to clients on multiple backend instances.

## Verification

Run `./mvnw test` with PostgreSQL and RabbitMQ available at the configured addresses.
The repository's Compose file currently provisions PostgreSQL only.

Automated messaging tests cover JSON round trips, Boot converter wiring and the real
listener adapter, after-commit publication, rollback/save failure, routing, the
frontend payload, and reply/root delivery order without requiring a live broker.

For live verification:

1. Start PostgreSQL and RabbitMQ, then the backend. Verify `message.exchange`,
   `message.queue`, their binding with `message.created`, and an active consumer
   in RabbitMQ Management UI. Check for any leftover demo String messages.
2. Subscribe two authorized frontend sessions to the same channel. Create a message;
   confirm its database row and one new-message event per subscribed session.
3. Reply to a thread; confirm one reply and the root reply-count update. Check
   mentions, editing, deletion, reactions, attachments, pins, and private-channel
   subscription restrictions.
4. Confirm queue messages are acknowledged and no conversion errors appear in logs.

In the implementation environment, the full suite compiled: 52 tests passed and 14
database-dependent tests errored because PostgreSQL on localhost:5433 was unavailable.
Application startup stopped for the same reason; Docker's engine and a local RabbitMQ
listener were unavailable, so live broker/frontend verification remains outstanding.
