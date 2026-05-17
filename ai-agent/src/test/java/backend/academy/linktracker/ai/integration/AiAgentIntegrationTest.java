package backend.academy.linktracker.ai.integration;

import backend.academy.linktracker.avro.LinkProcessedUpdateEvent;
import backend.academy.linktracker.avro.LinkRawUpdateEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.group-id=test-group",
    "app.kafka.topic.raw-updates=test-raw-updates",
    "app.kafka.topic.processed-updates=test-processed-updates",
    "app.ai-agent.filtering.stop-words[0]=spam",
    "app.ai-agent.filtering.excluded-authors[0]=bot-user",
    "app.ai-agent.filtering.min-length=10",
    "app.ai-agent.summarization.threshold=50",
    "app.ai-agent.summarization.mode=stub"
})
@EmbeddedKafka(
    topics = {"test-raw-updates", "test-processed-updates"},
    partitions = 1,
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:0",
        "port=0"
    }
)
@ActiveProfiles("test")
@DirtiesContext
class AiAgentIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, LinkRawUpdateEvent> testProducer;

    @Test
    void shouldProcessValidMessage() throws Exception {
        BlockingQueue<ConsumerRecord<String, LinkProcessedUpdateEvent>> records = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, LinkProcessedUpdateEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("test-processed-updates");
        containerProperties.setMessageListener((MessageListener<String, LinkProcessedUpdateEvent>) records::add);

        KafkaMessageListenerContainer<String, LinkProcessedUpdateEvent> container =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        LinkRawUpdateEvent rawEvent = LinkRawUpdateEvent.newBuilder()
                .setId(1L)
                .setUrl("http://example.com")
                .setDescription("This is a valid message without any stop words and it's long enough")
                .setAuthor("regular-user")
                .setTgChatIds(java.util.Arrays.asList(123L))
                .build();

        testProducer.send("test-raw-updates", String.valueOf(1L), rawEvent);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(records).isNotEmpty());

        ConsumerRecord<String, LinkProcessedUpdateEvent> received = records.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.key()).isEqualTo("1");
        assertThat(received.value().getId()).isEqualTo(1L);
        assertThat(received.value().getDescription()).contains("This is a valid message");
        assertThat(received.value().getPriority()).isEqualTo("HIGH");

        container.stop();
    }

    @TestConfiguration
    static class TestKafkaConfig {
        @Bean
        @Primary
        public KafkaTemplate<String, LinkRawUpdateEvent> testKafkaTemplate(EmbeddedKafkaBroker embeddedKafkaBroker) {
            Map<String, Object> producerProps = new HashMap<>();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        }
    }
}
