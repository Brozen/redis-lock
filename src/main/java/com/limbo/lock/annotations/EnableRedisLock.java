package com.limbo.lock.annotations;

import com.limbo.lock.redis.RedisLockAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用RedisLock
 * @author Brozen
 * @date 2019/5/21 6:17 PM
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(RedisLockAutoConfiguration.class)
public @interface EnableRedisLock {
}
