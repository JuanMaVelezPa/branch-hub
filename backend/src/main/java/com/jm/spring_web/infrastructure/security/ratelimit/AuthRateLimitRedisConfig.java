package com.jm.spring_web.infrastructure.security.ratelimit;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "security.auth-rate-limit", name = "redis-enabled", havingValue = "true")
public class AuthRateLimitRedisConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient authRateLimitRedisClient(
            @Value("${spring.data.redis.host:127.0.0.1}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.username:}") String username,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.ssl.enabled:false}") boolean ssl) {
        RedisURI.Builder builder = RedisURI.builder().withHost(host).withPort(port).withSsl(ssl);
        if (username != null && !username.isBlank()) {
            builder.withAuthentication(username, password != null ? password : "");
        } else if (password != null && !password.isEmpty()) {
            builder.withPassword(password.toCharArray());
        }
        return RedisClient.create(builder.build());
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> authRateLimitRedisConnection(RedisClient authRateLimitRedisClient) {
        return authRateLimitRedisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    public ProxyManager<String> authRateLimitProxyManager(
            StatefulRedisConnection<String, byte[]> authRateLimitRedisConnection) {
        return Bucket4jLettuce.casBasedBuilder(authRateLimitRedisConnection).build();
    }
}
