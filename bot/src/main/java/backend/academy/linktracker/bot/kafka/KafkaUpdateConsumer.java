package backend.academy.linktracker.bot.kafka;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.bot.dto.LinkUpdateRequest;
import backend.academy.linktracker.bot.service.IdempotencyService;
import backend.academy.linktracker.bot.service.UpdateNotificationService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUpdateConsumer {

    private final UpdateNotificationService notificationService;
    private final Optional<IdempotencyService> idempotencyService;

    @KafkaListener(
            topics = "${app.kafka.topic.link-updates}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void listen(
            @Payload LinkUpdateEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info(
                "Received update from Kafka - topic: {}, partition: {}, offset: {}, event: {}",
                topic,
                partition,
                offset,
                event);

        // Check idempotency if service is available
        if (idempotencyService.isPresent() && idempotencyService.get().isProcessed(event.getId())) {
            log.info("Message already processed, skipping: id={}", event.getId());
            return;
        }

        try {
            // Convert Avro to DTO
            LinkUpdateRequest update = new LinkUpdateRequest(
                    event.getId(),
                    event.getUrl().toString(),
                    event.getDescription().toString(),
                    event.getTgChatIds());

            notificationService.sendNotifications(update);
            idempotencyService.ifPresent(service -> service.markAsProcessed(event.getId()));
            log.info("Successfully processed update: {}", update);
        } catch (Exception e) {
            log.error("Failed to process update: {}", event, e);
            throw e;
        }
    }
}
