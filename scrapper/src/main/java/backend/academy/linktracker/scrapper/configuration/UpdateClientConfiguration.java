package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GrpcUpdateClient;
import backend.academy.linktracker.scrapper.client.KafkaUpdateClient;
import backend.academy.linktracker.scrapper.client.OutboxUpdateClient;
import backend.academy.linktracker.scrapper.client.UpdateClient;
import backend.academy.linktracker.scrapper.repository.jdbc.JdbcOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class UpdateClientConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.notification", name = "transport", havingValue = "http")
    public UpdateClient httpUpdateClient(@Value("${app.bot-client.base-url}") String baseUrl) {
        return new BotClient(baseUrl);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.notification", name = "transport", havingValue = "grpc")
    public UpdateClient grpcUpdateClient(
            @Value("${app.bot.grpc.host:localhost}") String botHost, @Value("${app.bot.grpc.port:9090}") int botPort) {
        return new GrpcUpdateClient(botHost, botPort);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.notification", name = "transport", havingValue = "kafka")
    public UpdateClient kafkaUpdateClient(
            KafkaTemplate<String, LinkUpdateEvent> kafkaTemplate,
            @Value("${app.kafka.topic.link-updates}") String topicName) {
        return new KafkaUpdateClient(kafkaTemplate, topicName);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.notification", name = "transport", havingValue = "outbox")
    public UpdateClient outboxUpdateClient(
            JdbcOutboxEventRepository outboxRepository,
            ObjectMapper objectMapper) {
        return new OutboxUpdateClient(outboxRepository, objectMapper);
    }
}
