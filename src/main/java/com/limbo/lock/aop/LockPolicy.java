package com.limbo.lock.aop;

import com.limbo.lock.RedisLock;
import com.limbo.lock.annotations.Locked;
import com.limbo.lock.exception.LockException;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Brozen
 * @date 2019/5/22 10:01 AM
 */
public enum LockPolicy {

    // 快速失败，获取不到锁立即抛出异常
    FAST_FAIL() {
        @Override
        public void doInLock(RedisLock redisLock, Locked locked, Runnable runnable) {
            super.doInLock(redisLock, locked, runnable);
            if (redisLock.tryLock()) {
                try {
                    runnable.run();
                } finally {
                    redisLock.unlock();
                }
            } else {
                throw new LockException("获取锁失败！", locked.lockName(), Thread.currentThread().getName());
            }
        }
    },

    // 自旋获取锁，一段时间后获取不到锁则抛出异常
    SPIN() {
        @Override
        public void doInLock(RedisLock redisLock, Locked locked, Runnable runnable) {
            super.doInLock(redisLock, locked, runnable);
            long spinMillis = locked.spinMillis();
            if (spinMillis <= 0) {// 自旋时间小于等于0时，当成快速失败锁处理
                FAST_FAIL.doInLock(redisLock, locked, runnable);
            } else {
                if (redisLock.tryLock(spinMillis, TimeUnit.MILLISECONDS)) {
                    try {
                        runnable.run();
                    } finally {
                        redisLock.unlock();
                    }
                } else {
                    throw new LockException("获取锁超时！", locked.lockName(), Thread.currentThread().getName());
                }
            }
        }
    },

    // 阻塞，未获取到锁之前不会继续执行
    BLOCK() {
        @Override
        public void doInLock(RedisLock redisLock, Locked locked, Runnable runnable) {
            super.doInLock(redisLock, locked, runnable);
            redisLock.lock();
            try {
                runnable.run();
            } finally {
                redisLock.unlock();
            }
        }
    };

    LockPolicy() {
    }

    public void doInLock(RedisLock redisLock, Locked locked, Runnable runnable) {
        Objects.requireNonNull(redisLock, "RedisLock cannot be null!");
        Objects.requireNonNull(locked, "LockAnnotation cannot be null!");
        Objects.requireNonNull(runnable, "Runnable cannot be null!");
    }
}
