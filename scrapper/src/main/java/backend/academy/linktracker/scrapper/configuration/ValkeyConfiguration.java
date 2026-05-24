package backend.academy.linktracker.scrapper.configuration;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "app.valkey", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ValkeyConfiguration {

    @Bean
    public ValkeyProperties valkeyProperties(
        @Value("${app.valkey.host:localhost}") String host,
        @Value("${app.valkey.port:6379}") int port,
        @Value("${app.valkey.password:}") String password,
        @Value("${app.valkey.ttl-seconds:300}") long ttlSeconds
    ) {
        return new ValkeyProperties(host, port, password, ttlSeconds);
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(ValkeyProperties properties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.host());
        config.setPort(properties.port());
        if (properties.password() != null && !properties.password().isBlank()) {
            config.setPassword(properties.password());
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(
        RedisConnectionFactory connectionFactory,
        ValkeyProperties properties
    ) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(properties.ttlSeconds()))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfig)
            .build();
    }

    public record ValkeyProperties(
        String host,
        int port,
        String password,
        long ttlSeconds
    ) {
        public ValkeyProperties() {
            this("localhost", 6379, null, 300);
        }
    }
}
