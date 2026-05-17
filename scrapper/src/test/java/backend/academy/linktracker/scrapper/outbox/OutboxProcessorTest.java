package backend.academy.linktracker.scrapper.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest;
import backend.academy.linktracker.scrapper.entity.OutboxEventEntity;
import backend.academy.linktracker.scrapper.repository.jdbc.JdbcOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private JdbcOutboxEventRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, LinkUpdateEvent> kafkaTemplate;

    private OutboxProcessor processor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        processor = new OutboxProcessor(outboxRepository, kafkaTemplate, objectMapper);
    }

    @Test
    void shouldProcessPendingEvents() throws Exception {
        LinkUpdateRequest request = new LinkUpdateRequest(1L, "https://github.com/test/repo", "Update", List.of(100L));
        String payload = objectMapper.writeValueAsString(request);

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(1L);
        event.setAggregateId(1L);
        event.setPayload(payload);
        event.setStatus(OutboxEventEntity.OutboxStatus.PENDING);
        event.setRetryCount(0);

        when(outboxRepository.findPendingEvents()).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), any(LinkUpdateEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        processor.processOutboxEvents();

        verify(outboxRepository).updateStatus(1L, OutboxEventEntity.OutboxStatus.PROCESSING);
        verify(kafkaTemplate).send(anyString(), anyString(), any(LinkUpdateEvent.class));
        verify(outboxRepository).updateProcessed(any(), any());
    }

    @Test
    void shouldSkipWhenNoPendingEvents() {
        when(outboxRepository.findPendingEvents()).thenReturn(List.of());

        processor.processOutboxEvents();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void shouldMarkAsFailedAfterMaxRetries() throws Exception {
        LinkUpdateRequest request = new LinkUpdateRequest(1L, "https://github.com/test/repo", "Update", List.of(100L));
        String payload = objectMapper.writeValueAsString(request);

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(1L);
        event.setAggregateId(1L);
        event.setPayload(payload);
        event.setStatus(OutboxEventEntity.OutboxStatus.PENDING);
        event.setRetryCount(2); // already 2 retries, next will be 3 = MAX

        when(outboxRepository.findPendingEvents()).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), any(LinkUpdateEvent.class)))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        processor.processOutboxEvents();

        verify(outboxRepository).updateFailed(any(), anyString(), any(Integer.class));
    }
}
