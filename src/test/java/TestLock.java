import com.limbo.lock.LockConfiguration;
import com.limbo.lock.LockContext;
import com.limbo.lock.RedisLock;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @author Brozen
 * @date 2019/5/21 3:19 PM
 */
public class TestLock {

    public static void main(String[] args) throws InterruptedException {

        // redis中可以保存多把锁，每把锁的全名是 lockPrefix::lockName
        LettuceConnectionFactory redisConnectionFactory = createRedisConnectionFactory();// or from Spring
        LockConfiguration configuration = LockConfiguration.builder().lockPrefix("RedisLockAA").build();
        LockContext context = new LockContext(configuration, redisConnectionFactory);

        // 锁名称是分布式环境下区分锁的标志
        RedisLock lock1 = context.createLock("TestLock");
        RedisLock lock2 = context.createLock("TestLock");

        int mockThreadCount = 10;
        CountDownLatch latch = new CountDownLatch(mockThreadCount);
        Random random = new Random();
        for (int i = 0; i < mockThreadCount; i++) {
            RedisLock lock = i % 2 == 0 ? lock1 : lock2;
            new Thread(() -> {
                lock.lock();
                System.out.println(Thread.currentThread().getName() + " 得到锁 " + lock);
                try {
                    Thread.sleep(random.nextInt(10000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                lock.unlock();
                System.out.println(Thread.currentThread().getName() + " 释放锁 " + lock);
                latch.countDown();
            }).start();
        }

        latch.await();
        lock1.close();
        lock2.close();
        redisConnectionFactory.destroy();
    }


    private static LettuceConnectionFactory createRedisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName("redis.brozen.top");
        configuration.setPort(6379);
        configuration.setPassword(RedisPassword.of("brozen@1995"));
        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration);
        factory.afterPropertiesSet();
        return factory;
    }

}
