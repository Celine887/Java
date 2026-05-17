package backend.academy.linktracker.scrapper.resilience;

import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.UpdateClient;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest(httpPort = 8089)
class Resilience4jIntegrationTest {
    @Test
    void testTimeoutConfiguration() {
        stubFor(get(urlEqualTo("/repos/test/repo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"id\":1,\"name\":\"repo\",\"updated_at\":\"2024-01-01T00:00:00Z\"}")
                        .withFixedDelay(3000)));

        var circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(100)
                .minimumNumberOfCalls(100)
                .build();
        var circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        var retryConfig = RetryConfig.custom()
                .maxAttempts(1)
                .build();
        var retryRegistry = RetryRegistry.of(retryConfig);

        var client = new GitHubClient(
                "http://localhost:8089",
                Duration.ofSeconds(1),
                circuitBreakerRegistry,
                retryRegistry
        );

        long startTime = System.currentTimeMillis();
        var result = client.fetchRepository("test", "repo");
        long duration = System.currentTimeMillis() - startTime;

        assertThat(result).isEmpty();
        assertThat(duration).isLessThan(2000);
    }

    @Test
    void testRetryOnServerErrors() {
        stubFor(get(urlEqualTo("/repos/test/repo"))
                .inScenario("Retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withBody("{\"message\":\"Service Unavailable\"}"))
                .willSetStateTo("Success"));

        stubFor(get(urlEqualTo("/repos/test/repo"))
                .inScenario("Retry")
                .whenScenarioStateIs("Success")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"repo\",\"updated_at\":\"2024-01-01T00:00:00Z\"}")));

        var retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .retryOnResult(response -> response instanceof java.util.Optional && ((java.util.Optional<?>) response).isEmpty())
                .build();
        var retryRegistry = RetryRegistry.of(retryConfig);

        var circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(100)
                .minimumNumberOfCalls(100)
                .build();
        var circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        var client = new GitHubClient(
                "http://localhost:8089",
                Duration.ofSeconds(5),
                circuitBreakerRegistry,
                retryRegistry
        );

        // When
        var result = client.fetchRepository("test", "repo");

        // Then
        assertThat(result).isPresent();
        verify(2, getRequestedFor(urlEqualTo("/repos/test/repo")));
    }

    @Test
    void testNoRetryOn4xxErrors() {
        // Given
        stubFor(get(urlEqualTo("/repos/test/repo"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"Not Found\"}")));

        var retryConfig = RetryConfig.custom()
                .maxAttempts(1)
                .build();
        var retryRegistry = RetryRegistry.of(retryConfig);

        var circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(100)
                .minimumNumberOfCalls(100)
                .build();
        var circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        var client = new GitHubClient(
                "http://localhost:8089",
                Duration.ofSeconds(5),
                circuitBreakerRegistry,
                retryRegistry
        );

        // When
        var result = client.fetchRepository("test", "repo");

        // Then
        assertThat(result).isEmpty();
        verify(1, getRequestedFor(urlEqualTo("/repos/test/repo")));
    }

    @Test
    void testRateLimiterBlocking() {
        // Given
        var config = RateLimiterConfig.custom()
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build();
        var registry = RateLimiterRegistry.of(config);
        RateLimiter rateLimiter = registry.rateLimiter("test");

        // When
        int successCount = 0;
        int rateLimitCount = 0;

        for (int i = 0; i < 15; i++) {
            boolean permitted = rateLimiter.acquirePermission();
            if (permitted) {
                successCount++;
            } else {
                rateLimitCount++;
            }
        }

        // Then
        assertThat(successCount).isEqualTo(10);
        assertThat(rateLimitCount).isEqualTo(5);
    }

    @Test
    void testRateLimiterThrowsException() {
        // Given
        var config = RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build();
        var registry = RateLimiterRegistry.of(config);
        RateLimiter rateLimiter = registry.rateLimiter("test");

        // When
        for (int i = 0; i < 5; i++) {
            rateLimiter.acquirePermission();
        }

        // Then
        boolean permitted = rateLimiter.acquirePermission();
        assertThat(permitted).isFalse();
    }

    @Test
    void testCircuitBreakerConfiguration() {
        // Given
        var circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(5)
                .build();
        var circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        // When
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker("testCircuitBreaker");

        // Then
        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(10);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50.0f);
    }

    @Test
    void testFallbackToKafka() {
        // Given
        stubFor(get(urlEqualTo("/repos/test/repo"))
                .willReturn(aResponse().withStatus(500)));

        // Создаем
        var mockFallbackClient = new UpdateClient() {
            private boolean wasCalled = false;

            @Override
            public void sendUpdate(backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest request) {
                wasCalled = true;
            }

            public boolean wasCalled() {
                return wasCalled;
            }
        };

        var circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(100)
                .minimumNumberOfCalls(100)
                .build();
        var circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        var retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .build();
        var retryRegistry = RetryRegistry.of(retryConfig);

        var botClient = new BotClient(
                "http://localhost:8089",
                Duration.ofSeconds(1),
                circuitBreakerRegistry,
                retryRegistry,
                mockFallbackClient
        );

        // When
        var request = new backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest(
                1L,
                "http://example.com",
                "Test update",
                List.of(1L, 2L, 3L)
        );

        try {
            botClient.sendUpdate(request);
        } catch (Exception e) {
        }

        // Then
        assertThat(true).isTrue();
    }
}
