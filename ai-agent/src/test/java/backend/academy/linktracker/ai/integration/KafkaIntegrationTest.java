package backend.academy.linktracker.ai.integration;

import backend.academy.linktracker.ai.model.LinkProcessedUpdateEvent;
import backend.academy.linktracker.ai.model.LinkRawUpdateEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@Import(TestKafkaConfiguration.class)
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"))
            .withReuse(true);

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.kafka.topic.raw-updates", () -> "link.raw-updates");
        registry.add("app.kafka.topic.processed-updates", () -> "link.processed-updates");
    }

    @BeforeAll
    static void setup() {
        Map<String, Object> config = new HashMap<>();
        config.put("bootstrap.servers", kafka.getBootstrapServers());
        try (AdminClient admin = AdminClient.create(config)) {
            admin.createTopics(List.of(
                    new NewTopic("link.raw-updates", 1, (short) 1),
                    new NewTopic("link.processed-updates", 1, (short) 1)
            )).all().get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
        }
    }

    @Autowired
    private KafkaTemplate<String, LinkRawUpdateEvent> kafkaTemplate;

    @Test
    @DisplayName("TC-3.1: Успешная публикация с HIGH приоритетом")
    void testHighPriorityPublication() throws Exception {
        // Given
        BlockingQueue<ConsumerRecord<String, LinkProcessedUpdateEvent>> records = new LinkedBlockingQueue<>();
        KafkaMessageListenerContainer<String, LinkProcessedUpdateEvent> container = createConsumer(records);
        container.start();
        Thread.sleep(2000); // Wait for consumer to be ready

        LinkRawUpdateEvent rawEvent = LinkRawUpdateEvent.builder()
                .id(1L)
                .description("Critical security vulnerability in authentication")
                .author("security-team")
                .tgChatIds(List.of(100L))
                .build();

        // When
        kafkaTemplate.send("link.raw-updates", String.valueOf(rawEvent.getId()), rawEvent);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(records).isNotEmpty());

        ConsumerRecord<String, LinkProcessedUpdateEvent> received = records.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.value().getId()).isEqualTo(1L);
        assertThat(received.value().getPriority().toString()).isEqualTo("HIGH");
        assertThat(received.value().getDescription()).contains("Critical security vulnerability");
        assertThat(received.value().getTgChatIds()).containsExactly(100L);

        container.stop();
    }

    @Test
    @DisplayName("TC-3.1: Успешная публикация с MEDIUM приоритетом")
    void testMediumPriorityPublication() throws Exception {
        // Given
        BlockingQueue<ConsumerRecord<String, LinkProcessedUpdateEvent>> records = new LinkedBlockingQueue<>();
        KafkaMessageListenerContainer<String, LinkProcessedUpdateEvent> container = createConsumer(records);
        container.start();
        Thread.sleep(2000);

        LinkRawUpdateEvent rawEvent = LinkRawUpdateEvent.builder()
                .id(2L)
                .description("Added new feature for user management")
                .author("dev-team")
                .tgChatIds(List.of(100L))
                .build();

        // When
        kafkaTemplate.send("link.raw-updates", String.valueOf(rawEvent.getId()), rawEvent);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(records).isNotEmpty());

        ConsumerRecord<String, LinkProcessedUpdateEvent> received = records.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.value().getId()).isEqualTo(2L);
        assertThat(received.value().getPriority().toString()).isEqualTo("MEDIUM");
        assertThat(received.value().getTgChatIds()).containsExactly(100L);

        container.stop();
    }

    @Test
    @DisplayName("TC-3.1: Успешная публикация с LOW приоритетом")
    void testLowPriorityPublication() throws Exception {
        // Given
        BlockingQueue<ConsumerRecord<String, LinkProcessedUpdateEvent>> records = new LinkedBlockingQueue<>();
        KafkaMessageListenerContainer<String, LinkProcessedUpdateEvent> container = createConsumer(records);
        container.start();
        Thread.sleep(2000);

        LinkRawUpdateEvent rawEvent = LinkRawUpdateEvent.builder()
                .id(3L)
                .description("Fix typo in documentation")
                .author("contributor")
                .tgChatIds(List.of(100L))
                .build();

        // When
        kafkaTemplate.send("link.raw-updates", String.valueOf(rawEvent.getId()), rawEvent);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(records).isNotEmpty());

        ConsumerRecord<String, LinkProcessedUpdateEvent> received = records.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.value().getId()).isEqualTo(3L);
        assertThat(received.value().getPriority().toString()).isEqualTo("LOW");
        assertThat(received.value().getDescription()).contains("typo");

        container.stop();
    }

    @Test
    @DisplayName("TC-3.2: Фильтрация spam сообщений")
    void testSpamFiltering() throws Exception {
        // Given
        BlockingQueue<ConsumerRecord<String, LinkProcessedUpdateEvent>> records = new LinkedBlockingQueue<>();
        KafkaMessageListenerContainer<String, LinkProcessedUpdateEvent> container = createConsumer(records);
        container.start();
        Thread.sleep(2000);

        LinkRawUpdateEvent rawEvent = LinkRawUpdateEvent.builder()
                .id(4L)
                .description("This is spam promotional content")
                .author("spammer")
                .tgChatIds(List.of(100L))
                .build();

        // When
        kafkaTemplate.send("link.raw-updates", String.valueOf(rawEvent.getId()), rawEvent);

        // Then - сообщение должно быть отфильтровано, не должно попасть в processed-updates
        Thread.sleep(3000);
        assertThat(records).isEmpty();

        container.stop();
    }

    @Test
    @DisplayName("TC-3.2: Фильтрация коротких сообщений")
    void testShortMessageFiltering() throws Exception {
        // Given
        BlockingQueue<ConsumerRecord<String, LinkProcessedUpdateEvent>> records = new LinkedBlockingQueue<>();
        KafkaMessageListenerContainer<String, LinkProcessedUpdateEvent> container = createConsumer(records);
        container.start();
        Thread.sleep(2000);

        LinkRawUpdateEvent rawEvent = LinkRawUpdateEvent.builder()
                .id(5L)
                .description("Short")
                .author("user")
                .tgChatIds(List.of(100L))
                .build();

        // When
        kafkaTemplate.send("link.raw-updates", String.valueOf(rawEvent.getId()), rawEvent);

        // Then
        Thread.sleep(3000);
        assertThat(records).isEmpty();

        container.stop();
    }

    @Test
    @DisplayName("TC-3.2: Фильтрация исключенных авторов")
    void testExcludedAuthorFiltering() throws Exception {
        // Given
        BlockingQueue<ConsumerRecord<String, LinkProcessedUpdateEvent>> records = new LinkedBlockingQueue<>();
        KafkaMessageListenerContainer<String, LinkProcessedUpdateEvent> container = createConsumer(records);
        container.start();
        Thread.sleep(2000);

        LinkRawUpdateEvent rawEvent = LinkRawUpdateEvent.builder()
                .id(6L)
                .description("This is a valid message with enough length")
                .author("bot-user")
                .tgChatIds(List.of(100L))
                .build();

        // When
        kafkaTemplate.send("link.raw-updates", String.valueOf(rawEvent.getId()), rawEvent);

        // Then
        Thread.sleep(3000);
        assertThat(records).isEmpty();

        container.stop();
    }

    private KafkaMessageListenerContainer<String, LinkProcessedUpdateEvent> createConsumer(
            BlockingQueue<ConsumerRecord<String, LinkProcessedUpdateEvent>> records) {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, LinkProcessedUpdateEvent.class.getName());

        DefaultKafkaConsumerFactory<String, LinkProcessedUpdateEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("link.processed-updates");
        containerProperties.setMessageListener((MessageListener<String, LinkProcessedUpdateEvent>) records::add);

        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }
}
