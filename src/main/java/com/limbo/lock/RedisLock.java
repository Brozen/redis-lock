package com.limbo.lock;

import com.limbo.lock.exception.LockCloseException;
import com.limbo.lock.exception.LockException;
import com.limbo.lock.exception.LockOperationException;
import com.limbo.lock.exception.UnlockException;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.types.Expiration;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

/**
 * @author Brozen
 * @date 2019/5/7 7:01 PM
 */
@Slf4j
public class RedisLock implements Lock, AutoCloseable {

    static final String LOCK_NAME_PREFIX_SPLITER = "::";
    // 字符集UTF-8
    private static final Charset UTF8 = Charset.forName("UTF-8");
    // 订阅锁过期事件后，用来取消订阅
    Disposable messageDisposable;
    // 锁名称，锁名称将作为key，在Redis中应当唯一
    private String lockName;
    private LockContext lockContext;
    private LockConfiguration lockConfiguration;
    private AtomicBoolean available;// 锁是否可用，close之后则不可用
    // 因获取锁失败而阻塞的线程
    private ConcurrentLinkedQueue<Thread> parkingThread;
    // 独占锁的线程，如果当前虚拟机中的线程占有锁，则此处引用占有锁的线程
    private AtomicReference<Thread> exclusiveOwnerThread;
    // 曾占有锁，但是过期的线程
    private Set<Thread> ownedAndTimeoutThread;

    RedisLock(LockContext context, String lockName, LockConfiguration configuration) {
        this.lockContext = context;
        this.lockConfiguration = configuration;
        this.available = new AtomicBoolean(true);

        this.parkingThread = new ConcurrentLinkedQueue<>();
        this.exclusiveOwnerThread = new AtomicReference<>();
        this.ownedAndTimeoutThread = Collections.synchronizedSet(new HashSet<>());

        this.lockName = configuration.getLockPrefix() + LOCK_NAME_PREFIX_SPLITER + lockName;
        this.messageDisposable = context.getMessageObservable()// observable发布的是上下文中所有锁过期事件，具体的锁名称需要过滤一下
                .filter(redisMessageInfo -> lockName.equals(redisMessageInfo.getKey()))
                .subscribe(redisMessageInfo -> {
                    System.out.println(redisMessageInfo.getMessage());

                    switch (redisMessageInfo.getMessage()) {
                        case EXPIRED:
                        case EVICT:
                            recordTimeoutIfHasExclusiveOwner();
                        case DELETE:
                            signalToCompeteLock();
                    }
                });

    }

    // 独占锁的线程名称，在线程名称在所有尝试获取分布式锁的线程名称中应当唯一
    private String getExclusiveOwnerName(Thread thread) {
        return thread == null ? null : lockContext.getLockContextUID() + "__" + thread.getName();
    }

    // 当锁过期时，如果当前虚拟机中的线程占有锁，需要记录锁过期的线程
    private void recordTimeoutIfHasExclusiveOwner() {
        Thread currentExclusiveThread = exclusiveOwnerThread.get();
        if (currentExclusiveThread != null) {
            exclusiveOwnerThread.compareAndSet(currentExclusiveThread, null);
            ownedAndTimeoutThread.add(currentExclusiveThread);
        }
    }

    // 需要唤醒阻塞的线程，重新竞争获取锁
    private void signalToCompeteLock() {
        Thread thread = parkingThread.poll();
        if (thread == null) {
            return;
        }
        LockSupport.unpark(thread);
    }

    // 断言锁处于可用状态
    private void assertLockAvailable(Supplier<LockOperationException> supplier) {
        if (!this.available.get()) {
            throw supplier.get();
        }
    }

    @Override
    public void lock() {
        if (tryLock()) {
            return;
        }

        Thread currentThread = Thread.currentThread();
        for (; ; ) {
            if (tryLock()) {
                return;
            }

            parkingThread.offer(currentThread);
            // 线程暂停一段时间后重新尝试获取锁，防止因redis未开启事件推送或网络延迟问题导致无法唤醒线程
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(lockConfiguration.getLockCompetitionParkInterval()));
            parkingThread.remove(currentThread);
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryLock()) {
            return;
        }

        Thread currentThread = Thread.currentThread();
        for (; ; ) {
            if (tryLock()) {
                return;
            }

            parkingThread.offer(currentThread);
            // 线程暂停一段时间后重新尝试获取锁，防止因redis未开启事件推送或网络延迟问题导致无法唤醒线程
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(lockConfiguration.getLockCompetitionParkInterval()));
            parkingThread.remove(currentThread);

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    @Override
    public boolean tryLock() {
        Thread currentThread = Thread.currentThread();
        assertLockAvailable(() -> new LockException("获取锁失败，当前锁已被销毁!", lockName, getExclusiveOwnerName(currentThread)));

        Boolean locked = lockContext.getConnection().set(lockName.getBytes(UTF8),
                getExclusiveOwnerName(currentThread).getBytes(UTF8),
                Expiration.seconds(lockConfiguration.getTenancySeconds()),
                RedisStringCommands.SetOption.SET_IF_ABSENT);
        if (locked) {
            exclusiveOwnerThread.set(currentThread);
        }
        return locked;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        if (tryLock()) {
            return true;
        }

        long deadline = System.nanoTime() + unit.toNanos(time);
        for (; ; ) {
            if (tryLock()) {
                return true;
            }

            if (deadline <= System.nanoTime()) {// 超时
                return false;
            }
        }
    }

    @Override
    public void unlock() {
        Thread currentThread = Thread.currentThread();
        assertLockAvailable(() -> new UnlockException("释放锁失败，当前锁已被销毁!", lockName, getExclusiveOwnerName(currentThread)));
        doUnlock(currentThread, true);
    }

    private void doUnlock(Thread thread, boolean internalThrowWhenTimeout) {
        // FIX 创建两把同名锁，在锁1申请，在锁2释放，居然能成功？
        if (exclusiveOwnerThread.get() == Thread.currentThread()
                && lockContext.getConnection().<Boolean>eval(
                        LockContext.UNLOCK_SCRIPT.getBytes(UTF8),
                        ReturnType.BOOLEAN, 1,
                        lockName.getBytes(UTF8),
                        getExclusiveOwnerName(thread).getBytes(UTF8)
                )
        ) {
            exclusiveOwnerThread.compareAndSet(thread, null);
            return;
        }

        // 解锁失败，有可能是锁租约时间过期，或已经解锁过
        if (ownedAndTimeoutThread.contains(thread)) {// 如果是超过租约时间，则线程会被放入过期set
            ownedAndTimeoutThread.remove(thread);
            if (internalThrowWhenTimeout && lockConfiguration.isThrowExceptionWhenUnlockTimeout()) {
                throw new UnlockException("锁租约过期！", lockName, getExclusiveOwnerName(exclusiveOwnerThread.get()));
            }
        } else {
            // 线程未被中断，则是已经失去锁的情况下再次解锁
            // 1. 是重复解锁，不作处理;
            // 2. 锁过期，线程还未加入过期set，但是redis已经清空了锁占有者，尝试解锁会到这里;
            log.warn("正在尝试解锁，但是当前线程并未占有锁，可能是重复解锁或锁租约过期！");
        }
    }

    @Override
    public Condition newCondition() {
        // TODO
        return null;
    }

    @Override
    public void close() {
        this.available.set(false);
        if (exclusiveOwnerThread.get() != null) {
            // 当前锁有线程在占用
            if (this.lockConfiguration.isUnlockWhenClose()) {
                doUnlock(exclusiveOwnerThread.get(), false);
            } else {// 否则抛出异常
                throw new LockCloseException("当前虚拟机占有锁，不允许销毁！", lockName, getExclusiveOwnerName(exclusiveOwnerThread.get()));
            }
        }

        // 当前锁有线程在等待
        if (!parkingThread.isEmpty()) {
            Thread t;
            while ((t = parkingThread.poll()) != null) {
                LockSupport.unpark(t);
            }
        }

        this.lockContext.disposeLock(this);
    }
}
