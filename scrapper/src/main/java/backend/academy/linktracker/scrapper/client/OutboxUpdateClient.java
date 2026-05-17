package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest;
import backend.academy.linktracker.scrapper.entity.OutboxEventEntity;
import backend.academy.linktracker.scrapper.repository.jdbc.JdbcOutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class OutboxUpdateClient implements UpdateClient {

    private final JdbcOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void sendUpdate(LinkUpdateRequest request) {
        try {
            String payload = objectMapper.writeValueAsString(request);

            OutboxEventEntity event = new OutboxEventEntity();
            event.setAggregateId(request.id());
            event.setEventType("LINK_UPDATE");
            event.setPayload(payload);
            event.setStatus(OutboxEventEntity.OutboxStatus.PENDING);

            outboxRepository.save(event);
            log.info("Saved link update to outbox: id={}", request.id());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize link update request", e);
            throw new RuntimeException("Failed to save update to outbox", e);
        }
    }
}
