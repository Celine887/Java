package backend.academy.linktracker.scrapper.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.scrapper.client.UpdateClient;
import backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {"app.notification.transport=kafka", "app.scheduler.enabled=false"})
class ScrapperToKafkaToBotIntegrationTest {

    static Network network = Network.newNetwork();

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"))
            .withNetwork(network)
            .withReuse(true);

    @Container
    static GenericContainer<?> schemaRegistry =
            new GenericContainer<>(DockerImageName.parse("confluentinc/cp-schema-registry:7.7.0"))
                    .withNetwork(network)
                    .withExposedPorts(8081)
                    .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9092")
                    .dependsOn(kafka);

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add(
                "spring.kafka.producer.properties.schema.registry.url",
                () -> "http://localhost:" + schemaRegistry.getMappedPort(8081));
    }

    @Autowired
    private UpdateClient updateClient;

    @Test
    void shouldSendUpdateFromScrapperThroughKafkaToBot() throws Exception {
        BlockingQueue<ConsumerRecord<String, LinkUpdateEvent>> records = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "bot-consumer-group-test");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        consumerProps.put("schema.registry.url", "http://localhost:" + schemaRegistry.getMappedPort(8081));
        consumerProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, LinkUpdateEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("link-updates");
        containerProperties.setMessageListener((MessageListener<String, LinkUpdateEvent>) records::add);

        KafkaMessageListenerContainer<String, LinkUpdateEvent> container =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.start();

        ContainerTestUtils.waitForAssignment(container, 1);

        LinkUpdateRequest request = new LinkUpdateRequest(
                100L,
                "https://github.com/test/integration-repo",
                "Integration test update: new PR merged",
                List.of(111L, 222L, 333L));

        updateClient.sendUpdate(request);

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(records).isNotEmpty());

        ConsumerRecord<String, LinkUpdateEvent> received = records.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.key()).isEqualTo("100");
        assertThat(received.value().getId()).isEqualTo(100L);
        assertThat(received.value().getUrl().toString()).isEqualTo("https://github.com/test/integration-repo");
        assertThat(received.value().getDescription().toString()).isEqualTo("Integration test update: new PR merged");
        assertThat(received.value().getTgChatIds()).containsExactly(111L, 222L, 333L);

        container.stop();
    }
}
