package backend.academy.linktracker.scrapper.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GrpcUpdateClient;
import backend.academy.linktracker.scrapper.client.KafkaUpdateClient;
import backend.academy.linktracker.scrapper.client.UpdateClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

class UpdateClientConfigurationTest {

    @SpringBootTest
    @TestPropertySource(properties = {"app.notification.transport=http", "app.scheduler.enabled=false"})
    static class HttpTransportTest {
        @Autowired
        private UpdateClient updateClient;

        @Test
        void shouldUseBotClientForHttpTransport() {
            assertThat(updateClient).isInstanceOf(BotClient.class);
        }
    }

    @SpringBootTest
    @TestPropertySource(properties = {"app.notification.transport=grpc", "app.scheduler.enabled=false"})
    static class GrpcTransportTest {
        @Autowired
        private UpdateClient updateClient;

        @Test
        void shouldUseGrpcClientForGrpcTransport() {
            assertThat(updateClient).isInstanceOf(GrpcUpdateClient.class);
        }
    }

    @SpringBootTest
    @Testcontainers
    @TestPropertySource(properties = {"app.notification.transport=kafka", "app.scheduler.enabled=false"})
    static class KafkaTransportTest {

        @Container
        static KafkaContainer kafka =
                new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0")).withReuse(true);

        @DynamicPropertySource
        static void kafkaProperties(DynamicPropertyRegistry registry) {
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        }

        @Autowired
        private UpdateClient updateClient;

        @Test
        void shouldUseKafkaClientForKafkaTransport() {
            assertThat(updateClient).isInstanceOf(KafkaUpdateClient.class);
        }
    }

    @SpringBootTest
    @Testcontainers
    @TestPropertySource(properties = {"app.scheduler.enabled=false"})
    static class DefaultTransportTest {

        @Container
        static KafkaContainer kafka =
                new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0")).withReuse(true);

        @DynamicPropertySource
        static void kafkaProperties(DynamicPropertyRegistry registry) {
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        }

        @Autowired
        private UpdateClient updateClient;

        @Test
        void shouldUseKafkaClientByDefault() {
            assertThat(updateClient).isInstanceOf(KafkaUpdateClient.class);
        }
    }
}
