# redis-locked

#### 介绍
基于Redis实现的分布式锁

#### 使用
Redis可以开启expired\evict\delete的事件监听，帮助更好的唤醒锁；不开也可以用；

RedisLock继承自java.util.concurrent.locks.Lock，可以像使用ReentrantLock一样使用，不同的是初始化使用LockContext.createLock，并且在使用完毕后应关闭锁

```java
// 一个虚拟机中可以存在多个LockContext
LockConfiguration configuration = LockConfiguration.builder().lockPrefix("RedisLockAA").build();
LockContext context = new LockContext(configuration, redisConnectionFactory);
// 锁从LockContext初始化
RedisLock locked = context.createLock("TestLock");
locked.locked();
locked.unlock();
// 使用后应该关闭锁，释放锁占有的redis资源
locked.close();
```

#### 在SpringBoot中使用
1. 首先在pom中引入spring-data-redis；
```java
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-redis</artifactId>
    <version>${spring.boot.version}</version>
</dependency>
```

2. 在启动类上添加注解 @EnableRedisLock；
```$xslt
@SpringBootApplication
@EnableRedisLock
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}
```

3. 在需要加锁的方法上添加注解 @Locked，即可使使方法同步；锁定策略等参考@Locked注解属性的注释；


#### 待实现

1. 锁租约到期自动续约？
2. 实现condition实现分布式多线程同步？
