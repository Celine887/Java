package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.BaseIntegrationTest;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.ChatService;
import backend.academy.linktracker.scrapper.service.LinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@TestPropertySource(properties = {
    "app.valkey.enabled=true",
    "app.valkey.host=localhost",
    "app.valkey.port=6379",
    "app.valkey.ttl-seconds=2",
    "app.scheduler.enabled=false",
    "app.notification.transport=http"
})
class ValkeyCacheIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LinkService linkService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Long TEST_CHAT_ID = 12345L;
    private static final String TEST_URL = "https://github.com/test/repo";

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        Objects.requireNonNull(cacheManager.getCache("links")).clear();
        
        // Clear all data from database to avoid conflicts between tests
        linkRepository.findAllByChatId(TEST_CHAT_ID).forEach(link -> 
            linkRepository.deleteByChatIdAndUrl(TEST_CHAT_ID, link.url())
        );
        
        // Register chat if not exists
        if (!chatService.exists(TEST_CHAT_ID)) {
            chatService.register(TEST_CHAT_ID);
        }
    }

    @Test
    void shouldCacheLinksInValkey() {
        // Given: Add a link
        linkService.add(new AddLinkRequest(TEST_CHAT_ID, TEST_URL, Set.of()));

        // When: First call - should hit database
        ListLinksResponse firstResponse = linkService.listAll(TEST_CHAT_ID, null);

        // Then: Verify data is in cache
        var cache = cacheManager.getCache("links");
        assertThat(cache).isNotNull();
        assertThat(cache.get(TEST_CHAT_ID)).isNotNull();

        // When: Second call - should hit cache
        ListLinksResponse secondResponse = linkService.listAll(TEST_CHAT_ID, null);

        // Then: Both responses should be equal
        assertThat(secondResponse.links()).hasSize(firstResponse.links().size());
        assertThat(secondResponse.size()).isEqualTo(firstResponse.size());
    }

    @Test
    void shouldInvalidateCacheOnAddLink() {
        // Given: Initial state with cached data
        linkService.listAll(TEST_CHAT_ID, null);
        var cache = cacheManager.getCache("links");
        assertThat(cache).isNotNull();
        assertThat(cache.get(TEST_CHAT_ID)).isNotNull();

        // When: Add a new link
        linkService.add(new AddLinkRequest(TEST_CHAT_ID, TEST_URL, Set.of()));

        // Then: Cache should be invalidated
        assertThat(cache.get(TEST_CHAT_ID)).isNull();
    }

    @Test
    void shouldInvalidateCacheOnRemoveLink() {
        // Given: Add a link and cache the list
        linkService.add(new AddLinkRequest(TEST_CHAT_ID, TEST_URL, Set.of()));
        linkService.listAll(TEST_CHAT_ID, null);
        
        var cache = cacheManager.getCache("links");
        assertThat(cache).isNotNull();
        assertThat(cache.get(TEST_CHAT_ID)).isNotNull();

        // When: Remove the link
        linkService.remove(new RemoveLinkRequest(TEST_CHAT_ID, TEST_URL));

        // Then: Cache should be invalidated
        assertThat(cache.get(TEST_CHAT_ID)).isNull();
    }

    @Test
    void shouldExpireCacheAfterTTL() {
        // Given: Add a link and cache the list
        linkService.add(new AddLinkRequest(TEST_CHAT_ID, TEST_URL, Set.of()));
        linkService.listAll(TEST_CHAT_ID, null);

        // Verify cache key exists in Redis
        String cacheKey = "links::" + TEST_CHAT_ID;
        Boolean hasKey = redisTemplate.hasKey(cacheKey);
        assertThat(hasKey).isTrue();

        // Check TTL value in Redis
        Long ttl = redisTemplate.getExpire(cacheKey, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(ttl).isGreaterThan(0).isLessThanOrEqualTo(2);

        // When: Wait for TTL to expire (2 seconds + buffer)
        await().atMost(5, SECONDS)
            .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                // Then: Cache key should be expired in Redis
                Boolean keyExists = redisTemplate.hasKey(cacheKey);
                assertThat(keyExists).isFalse();
            });
    }

    @Test
    void shouldStoreDataInCorrectFormat() {
        // Given: Add a link
        linkService.add(new AddLinkRequest(TEST_CHAT_ID, TEST_URL, Set.of("tag1", "tag2")));

        // When: Cache the list
        ListLinksResponse response = linkService.listAll(TEST_CHAT_ID, null);

        // Then: Verify data format in cache
        var cache = cacheManager.getCache("links");
        assertThat(cache).isNotNull();
        
        var cachedValue = cache.get(TEST_CHAT_ID, ListLinksResponse.class);
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.links()).hasSize(1);
        assertThat(cachedValue.links().get(0).url()).isEqualTo(TEST_URL);
        assertThat(cachedValue.links().get(0).tags()).containsExactlyInAnyOrder("tag1", "tag2");
    }
}
