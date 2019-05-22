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


#### 待实现

1. 结合到SpringBoot中，添加注解支持；
2. 实现condition实现分布式多线程同步？
