package com.limbo.lock.redis;

import com.limbo.lock.LockConfiguration;
import com.limbo.lock.LockContext;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * @author Brozen
 * @date 2019/5/21 5:49 PM
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties(LockConfiguration.class)
public class RedisLockAutoConfiguration {

    private final LockConfiguration lockConfiguration;

    public RedisLockAutoConfiguration(LockConfiguration lockConfiguration) {
        this.lockConfiguration = lockConfiguration;
    }

    @Bean
    public LockContext lockContext(RedisConnectionFactory redisConnectionFactory) {
        return new LockContext(lockConfiguration, redisConnectionFactory);
    }

}
