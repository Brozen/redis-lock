package com.limbo.lock.exception;

/**
 * 申请锁异常，可能原因：
 * 锁已经销毁
 * @author Brozen
 * @date 2019/5/8 1:46 PM
 */
public class LockException extends LockOperationException {

    public LockException(String message, String lockName, String ownerName) {
        super(message, lockName, ownerName);
    }
}
