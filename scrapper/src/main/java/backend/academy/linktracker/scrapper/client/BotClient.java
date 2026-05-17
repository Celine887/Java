package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public class BotClient implements UpdateClient {
    private static final Logger log = LoggerFactory.getLogger(BotClient.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final UpdateClient fallbackClient;

    public BotClient(
            String baseUrl,
            Duration timeout,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            UpdateClient fallbackClient) {
        
        var requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(timeout);
        
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("botClient");
        this.retry = retryRegistry.retry("botClient");
        this.fallbackClient = fallbackClient;
    }

    @Override
    public void sendUpdate(LinkUpdateRequest request) {
        Supplier<Void> supplier = () -> {
            log.info("Sending update to bot via REST: url={}, chatIds={}", request.url(), request.tgChatIds());
            restClient
                    .post()
                    .uri("/updates")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, response) -> {
                        log.error(
                                "Bot API error: status={}, body={}",
                                response.getStatusCode(),
                                new String(response.getBody().readAllBytes()));
                    })
                    .toBodilessEntity();
            return null;
        };
        
        try {
            Retry.decorateSupplier(retry, 
                    CircuitBreaker.decorateSupplier(circuitBreaker, supplier)).get();
        } catch (Exception e) {
            log.warn("HTTP transport failed, falling back to Kafka: url={}", request.url());
            fallbackClient.sendUpdate(request);
        }
    }
}
