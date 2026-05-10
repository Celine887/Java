package backend.academy.linktracker.scrapper.configuration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Redis/Valkey Client-Side Caching (CSC).
 * CSC reduces network traffic by maintaining a local cache on the client side
 * and receiving invalidation notifications from the server via CLIENT TRACKING.
 * 
 * This implementation uses two Redis connections:
 * 1. Data connection - for normal Redis operations (GET, SET, etc.)
 * 2. Pub/Sub connection - for receiving invalidation notifications
 * 
 * When a key is modified on the server, Redis sends an invalidation message
 * to the pub/sub connection, which removes the key from the local cache.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "app.valkey.client-side-caching", name = "enabled", havingValue = "true")
public class ClientSideCachingConfiguration {

    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> dataConnection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    @Bean
    public ClientSideCache clientSideCache(ValkeyConfiguration.ValkeyProperties properties) {
        log.info("Initializing Client-Side Caching for Valkey");
        
        try {
            // Create Redis URI
            RedisURI redisUri = RedisURI.builder()
                .withHost(properties.host())
                .withPort(properties.port())
                .build();
            
            if (properties.password() != null && !properties.password().isBlank()) {
                redisUri.setPassword(properties.password().toCharArray());
            }

            // Create Redis client
            this.redisClient = RedisClient.create(redisUri);
            
            // Create data connection
            this.dataConnection = redisClient.connect();
            
            // Create pub/sub connection for invalidation messages
            this.pubSubConnection = redisClient.connectPubSub();
            
            // Create and setup client-side cache
            ClientSideCache cache = new ClientSideCache(dataConnection, pubSubConnection);
            cache.setupTracking();
            
            log.info("Client-Side Caching initialized successfully");
            return cache;
        } catch (Exception e) {
            log.error("Failed to initialize Client-Side Caching: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize Client-Side Caching", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down Client-Side Caching connections");
        try {
            if (dataConnection != null && dataConnection.isOpen()) {
                dataConnection.close();
            }
            if (pubSubConnection != null && pubSubConnection.isOpen()) {
                pubSubConnection.close();
            }
            if (redisClient != null) {
                redisClient.shutdown();
            }
        } catch (Exception e) {
            log.warn("Error during Client-Side Caching cleanup: {}", e.getMessage());
        }
    }

    /**
     * Client-Side Cache implementation using Redis CLIENT TRACKING protocol.
     * Maintains a local cache and listens for invalidation messages via Pub/Sub.
     */
    @Slf4j
    public static class ClientSideCache {
        private final StatefulRedisConnection<String, String> dataConnection;
        private final StatefulRedisPubSubConnection<String, String> pubSubConnection;
        private final Map<String, String> localCache = new ConcurrentHashMap<>();
        private volatile boolean trackingEnabled = false;

        public ClientSideCache(
            StatefulRedisConnection<String, String> dataConnection,
            StatefulRedisPubSubConnection<String, String> pubSubConnection
        ) {
            this.dataConnection = dataConnection;
            this.pubSubConnection = pubSubConnection;
        }

        /**
         * Setup CLIENT TRACKING for automatic cache invalidation.
         * Uses RESP2 compatible approach with Pub/Sub for invalidation messages.
         */
        public void setupTracking() {
            try {
                log.debug("Setting up CLIENT TRACKING...");
                
                // Subscribe to invalidation channel
                RedisPubSubCommands<String, String> pubsub = pubSubConnection.sync();
                pubsub.subscribe("__redis__:invalidate");
                log.debug("Subscribed to __redis__:invalidate channel");

                // Add listener for invalidation messages
                pubSubConnection.addListener(new RedisPubSubAdapter<String, String>() {
                    @Override
                    public void message(String channel, String key) {
                        if ("__redis__:invalidate".equals(channel) && key != null) {
                            String removed = localCache.remove(key);
                            if (removed != null) {
                                log.debug("CSC: Invalidated key: {}", key);
                            }
                        }
                    }
                });

                // Get pub/sub client ID for redirect
                Long pubSubClientId = pubSubConnection.sync().clientId();
                log.debug("Pub/Sub client ID: {}", pubSubClientId);
                
                // Enable CLIENT TRACKING with redirect to pub/sub connection
                // This tells Redis to send invalidation messages to the pub/sub connection
                try {
                    dataConnection.sync().dispatch(
                        CommandType.CLIENT,
                        new StatusOutput<>(StringCodec.UTF8),
                        new CommandArgs<>(StringCodec.UTF8)
                            .add("TRACKING")
                            .add("ON")
                            .add("REDIRECT")
                            .add(pubSubClientId)
                    );
                    trackingEnabled = true;
                    log.info("CLIENT TRACKING enabled with redirect to client ID: {}", pubSubClientId);
                } catch (Exception e) {
                    log.warn("CLIENT TRACKING command failed (may not be supported): {}", e.getMessage());
                    log.info("Client-Side Caching will work in manual mode without automatic invalidation");
                }
            } catch (Exception e) {
                log.error("Failed to setup CLIENT TRACKING: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to setup Client-Side Caching", e);
            }
        }

        /**
         * Get value from local cache or fetch from Redis if not cached.
         * Automatically registers the key for tracking if CLIENT TRACKING is enabled.
         */
        public String get(String key) {
            // Check local cache first
            String cached = localCache.get(key);
            if (cached != null) {
                log.debug("CSC HIT: {}", key);
                return cached;
            }

            // Fetch from Redis
            String value = dataConnection.sync().get(key);
            if (value != null) {
                localCache.put(key, value);
                log.debug("CSC MISS: {} (fetched and cached)", key);
            } else {
                log.debug("CSC MISS: {} (key not found in Redis)", key);
            }
            return value;
        }

        /**
         * Set value in Redis. The local cache is immediately invalidated
         * to ensure consistency.
         */
        public void set(String key, String value) {
            dataConnection.sync().set(key, value);
            // Always invalidate local cache immediately to ensure consistency
            localCache.remove(key);
            log.debug("CSC SET: {}", key);
        }

        /**
         * Delete key from Redis. The local cache is immediately invalidated.
         */
        public void delete(String key) {
            dataConnection.sync().del(key);
            // Always invalidate local cache immediately to ensure consistency
            localCache.remove(key);
            log.debug("CSC DELETE: {}", key);
        }

        /**
         * Get current size of local cache.
         */
        public int getCacheSize() {
            return localCache.size();
        }

        /**
         * Clear local cache.
         */
        public void clearLocalCache() {
            int size = localCache.size();
            localCache.clear();
            log.debug("CSC: Local cache cleared ({} entries removed)", size);
        }

        /**
         * Check if CLIENT TRACKING is enabled.
         */
        public boolean isTrackingEnabled() {
            return trackingEnabled;
        }

        /**
         * Get cache statistics.
         */
        public CacheStats getStats() {
            return new CacheStats(localCache.size(), trackingEnabled);
        }
    }

    /**
     * Cache statistics record.
     */
    public record CacheStats(int localCacheSize, boolean trackingEnabled) {
    }
}
