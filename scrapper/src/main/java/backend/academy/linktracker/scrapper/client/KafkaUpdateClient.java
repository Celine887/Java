package backend.academy.linktracker.scrapper.client;


import backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class KafkaUpdateClient implements UpdateClient {

    private final KafkaTemplate<String, LinkUpdateRequest> kafkaTemplate;
    private final String topicName;

    @Override
    public void sendUpdate(LinkUpdateRequest request) {
        log.info("Sending update to Kafka topic {}: {}", topicName, request);

        kafkaTemplate.send(topicName, String.valueOf(request.id()), request).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send update to Kafka: {}", request, ex);
            } else {
                log.info("Successfully sent update to Kafka: {}", result.getRecordMetadata());
            }
        });
    }
}
