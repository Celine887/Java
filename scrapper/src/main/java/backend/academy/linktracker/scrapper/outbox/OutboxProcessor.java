package backend.academy.linktracker.scrapper.outbox;


import backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest;
import backend.academy.linktracker.scrapper.entity.OutboxEventEntity;
import backend.academy.linktracker.scrapper.repository.jdbc.JdbcOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notification", name = "transport", havingValue = "outbox")
public class OutboxProcessor {

    private final JdbcOutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, LinkUpdateRequest> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicName = "link-updates";
    private static final int MAX_RETRY_COUNT = 3;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:5000}")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEventEntity> pendingEvents = outboxRepository.findPendingEvents();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEventEntity event : pendingEvents) {
            try {
                processEvent(event);
            } catch (Exception e) {
                log.error("Failed to process outbox event: id={}", event.getId(), e);
                handleFailure(event, e);
            }
        }
    }

    private void processEvent(OutboxEventEntity event) throws Exception {
        outboxRepository.updateStatus(event.getId(), OutboxEventEntity.OutboxStatus.PROCESSING);

        LinkUpdateRequest request = objectMapper.readValue(event.getPayload(), LinkUpdateRequest.class);

        kafkaTemplate.send(topicName, String.valueOf(request.id()), request).get();

        outboxRepository.updateProcessed(event.getId(), OffsetDateTime.now());
        log.info("Successfully processed outbox event: id={}", event.getId());
    }

    private void handleFailure(OutboxEventEntity event, Exception e) {
        int newRetryCount = event.getRetryCount() + 1;

        if (newRetryCount >= MAX_RETRY_COUNT) {
            outboxRepository.updateFailed(event.getId(), e.getMessage(), newRetryCount);
            log.error("Outbox event failed after {} retries: id={}", MAX_RETRY_COUNT, event.getId());
        } else {
            outboxRepository.updateStatus(event.getId(), OutboxEventEntity.OutboxStatus.PENDING);
            log.warn("Outbox event will be retried: id={}, retryCount={}", event.getId(), newRetryCount);
        }
    }
}
