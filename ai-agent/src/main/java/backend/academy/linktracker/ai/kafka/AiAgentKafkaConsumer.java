package backend.academy.linktracker.ai.kafka;

import backend.academy.linktracker.ai.model.LinkProcessedUpdateEvent;
import backend.academy.linktracker.ai.model.LinkRawUpdateEvent;
import backend.academy.linktracker.ai.service.UpdateProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAgentKafkaConsumer {

    private final UpdateProcessingService updateProcessingService;
    private final KafkaTemplate<String, LinkProcessedUpdateEvent> kafkaTemplate;
    private final String processedUpdatesTopic;

    @KafkaListener(
            topics = "${app.kafka.topic.raw-updates}",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(
            @Payload LinkRawUpdateEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        try {
            log.info("Received message from topic {} partition {} offset {}: id={}, author={}",
                    topic, partition, offset, event.getId(), event.getAuthor());

            LinkProcessedUpdateEvent processedEvent = updateProcessingService.processUpdate(event);

            if (processedEvent != null) {
                kafkaTemplate.send(processedUpdatesTopic, String.valueOf(event.getId()), processedEvent)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to send processed event {} to topic {}",
                                        event.getId(), processedUpdatesTopic, ex);
                            } else {
                                log.info("Successfully sent processed event {} to topic {}",
                                        event.getId(), processedUpdatesTopic);
                            }
                        });
            } else {
                log.info("Event {} was filtered out, not sending to processed topic", event.getId());
            }

        } catch (Exception e) {
            log.error("Error processing message from topic {} partition {} offset {}: {}",
                    topic, partition, offset, event, e);
            throw e;
        }
    }
}
