package com.limbo.lock.exception;

/**
 * 解锁异常，可能原因：
 * 锁租约过期
 * @author Brozen
 * @date 2019/5/8 1:46 PM
 */
public class UnlockException extends LockOperationException {

    public UnlockException(String message, String lockName, String ownerName) {
        super(message, lockName, ownerName);
    }
}
