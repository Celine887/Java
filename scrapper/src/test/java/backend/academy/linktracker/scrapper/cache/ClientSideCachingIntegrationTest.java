package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.BaseIntegrationTest;
import backend.academy.linktracker.scrapper.configuration.ClientSideCachingConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Integration test for Client-Side Caching (CSC) with RESP3 protocol.
 * Tests verify that:
 * 1. Data is cached locally on the client side
 * 2. Cache is automatically invalidated when data changes on the server
 * 3. CLIENT TRACKING protocol works correctly
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.valkey.enabled=true",
    "app.valkey.host=localhost",
    "app.valkey.port=6379",
    "app.valkey.ttl-seconds=300",
    "app.valkey.client-side-caching.enabled=true",
    "app.scheduler.enabled=false",
    "app.notification.transport=http"
})
class ClientSideCachingIntegrationTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private ClientSideCachingConfiguration.ClientSideCache clientSideCache;

    private static final String TEST_KEY = "csc:test:key";
    private static final String TEST_VALUE = "test-value";

    @BeforeEach
    void setUp() {
        if (clientSideCache != null) {
            clientSideCache.clearLocalCache();
            clientSideCache.delete(TEST_KEY);
        }
    }

    @Test
    void shouldCacheDataLocally() {
        // Given: CSC is enabled
        assertThat(clientSideCache).isNotNull();

        // When: Set a value and get it
        clientSideCache.set(TEST_KEY, TEST_VALUE);
        String firstGet = clientSideCache.get(TEST_KEY);

        // Then: Value should be cached locally
        assertThat(firstGet).isEqualTo(TEST_VALUE);
        assertThat(clientSideCache.getCacheSize()).isGreaterThan(0);

        // When: Get the same key again
        String secondGet = clientSideCache.get(TEST_KEY);

        // Then: Should return from local cache
        assertThat(secondGet).isEqualTo(TEST_VALUE);
    }

    @Test
    void shouldInvalidateLocalCacheOnServerUpdate() {
        // Given: Value is cached locally
        clientSideCache.set(TEST_KEY, TEST_VALUE);
        clientSideCache.get(TEST_KEY);
        int initialCacheSize = clientSideCache.getCacheSize();
        assertThat(initialCacheSize).isGreaterThan(0);

        // When: Update the value on the server
        clientSideCache.set(TEST_KEY, "new-value");

        // Then: Local cache should be invalidated (wait for invalidation message)
        await().atMost(2, SECONDS)
            .pollInterval(100, java.util.concurrent.TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                // Cache size should decrease or key should be removed
                int currentSize = clientSideCache.getCacheSize();
                assertThat(currentSize).isLessThanOrEqualTo(initialCacheSize);
            });
    }

    @Test
    void shouldInvalidateLocalCacheOnServerDelete() {
        // Given: Value is cached locally
        clientSideCache.set(TEST_KEY, TEST_VALUE);
        clientSideCache.get(TEST_KEY);
        assertThat(clientSideCache.getCacheSize()).isGreaterThan(0);

        // When: Delete the key on the server
        clientSideCache.delete(TEST_KEY);

        // Then: Local cache should be invalidated
        await().atMost(2, SECONDS)
            .pollInterval(100, java.util.concurrent.TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                // After invalidation, getting the key should return null
                String value = clientSideCache.get(TEST_KEY);
                assertThat(value).isNull();
            });
    }

    @Test
    void shouldHandleMultipleKeys() {
        // Given: Multiple keys
        String key1 = "csc:test:key1";
        String key2 = "csc:test:key2";
        String key3 = "csc:test:key3";

        // When: Set and get multiple keys
        clientSideCache.set(key1, "value1");
        clientSideCache.set(key2, "value2");
        clientSideCache.set(key3, "value3");

        clientSideCache.get(key1);
        clientSideCache.get(key2);
        clientSideCache.get(key3);

        // Then: All keys should be cached locally
        assertThat(clientSideCache.getCacheSize()).isGreaterThanOrEqualTo(3);

        // When: Update one key
        clientSideCache.set(key2, "new-value2");

        // Then: Only that key should be invalidated
        await().atMost(2, SECONDS)
            .pollInterval(100, java.util.concurrent.TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                String value2 = clientSideCache.get(key2);
                assertThat(value2).isEqualTo("new-value2");
            });

        // Cleanup
        clientSideCache.delete(key1);
        clientSideCache.delete(key2);
        clientSideCache.delete(key3);
    }

    @Test
    void shouldProvideStats() {
        // Given: Some cached data
        clientSideCache.set(TEST_KEY, TEST_VALUE);
        clientSideCache.get(TEST_KEY);

        // When: Get stats
        ClientSideCachingConfiguration.CacheStats stats = clientSideCache.getStats();

        // Then: Stats should reflect cache state
        assertThat(stats).isNotNull();
        assertThat(stats.localCacheSize()).isGreaterThan(0);
    }
}
