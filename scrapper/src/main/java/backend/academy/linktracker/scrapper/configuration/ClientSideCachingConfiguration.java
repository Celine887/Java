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
            RedisURI redisUri = RedisURI.builder()
                .withHost(properties.host())
                .withPort(properties.port())
                .build();
            
            if (properties.password() != null && !properties.password().isBlank()) {
                redisUri.setPassword(properties.password().toCharArray());
            }

            this.redisClient = RedisClient.create(redisUri);
            
            this.dataConnection = redisClient.connect();
            
            this.pubSubConnection = redisClient.connectPubSub();
            
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
        public void setupTracking() {
            try {
                log.debug("Setting up CLIENT TRACKING...");
                
                RedisPubSubCommands<String, String> pubsub = pubSubConnection.sync();
                pubsub.subscribe("__redis__:invalidate");
                log.debug("Subscribed to __redis__:invalidate channel");

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

                Long pubSubClientId = pubSubConnection.sync().clientId();
                log.debug("Pub/Sub client ID: {}", pubSubClientId);
                
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

        public String get(String key) {
            String cached = localCache.get(key);
            if (cached != null) {
                log.debug("CSC HIT: {}", key);
                return cached;
            }

            String value = dataConnection.sync().get(key);
            if (value != null) {
                localCache.put(key, value);
                log.debug("CSC MISS: {} (fetched and cached)", key);
            } else {
                log.debug("CSC MISS: {} (key not found in Redis)", key);
            }
            return value;
        }

    
        public void set(String key, String value) {
            dataConnection.sync().set(key, value);
            localCache.remove(key);
            log.debug("CSC SET: {}", key);
        }

    
        public void delete(String key) {
            dataConnection.sync().del(key);
            localCache.remove(key);
            log.debug("CSC DELETE: {}", key);
        }

        public int getCacheSize() {
            return localCache.size();
        }

        public void clearLocalCache() {
            int size = localCache.size();
            localCache.clear();
            log.debug("CSC: Local cache cleared ({} entries removed)", size);
        }

        public boolean isTrackingEnabled() {
            return trackingEnabled;
        }

 
        public CacheStats getStats() {
            return new CacheStats(localCache.size(), trackingEnabled);
        }
    }


    public record CacheStats(int localCacheSize, boolean trackingEnabled) {
    }
}
