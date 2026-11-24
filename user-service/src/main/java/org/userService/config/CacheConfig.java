package org.userService.config;

import org.userService.dto.user.UserWithCardsResponse;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    var objectMapper = JsonMapper.builder().build();

    var serializer = new JacksonJsonRedisSerializer<>(
        objectMapper,
        UserWithCardsResponse.class
    );

    RedisCacheConfiguration cacheConfiguration =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(serializer)
            );

    return RedisCacheManager.builder(connectionFactory)
        .withInitialCacheConfigurations(
            Map.of("userWithCards", cacheConfiguration)
        )
        .cacheDefaults(cacheConfiguration)
        .transactionAware()
        .build();
  }
}