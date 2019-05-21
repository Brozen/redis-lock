package com.limbo.lock.exception;

/**
 * 销毁锁异常
 * @author Brozen
 * @date 2019/5/8 1:46 PM
 */
public class LockCloseException extends LockOperationException {

    public LockCloseException(String message, String lockName, String ownerName) {
        super(message, lockName, ownerName);
    }
}
