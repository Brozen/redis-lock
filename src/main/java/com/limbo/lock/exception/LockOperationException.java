package com.limbo.lock.exception;

import lombok.Getter;

/**
 * 锁操作异常
 * @author Brozen
 * @date 2019/5/8 1:46 PM
 */
public class LockOperationException extends RuntimeException {

    @Getter
    private String lockName;

    @Getter
    private String ownerName;

    public LockOperationException(String message, String lockName, String ownerName) {
        super(message + " lockName:" + lockName + " ownerName:" + ownerName);
        this.lockName = lockName;
        this.ownerName = ownerName;
    }

}
