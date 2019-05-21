package com.limbo.lock;

/**
 * @author Brozen
 * @date 2019/5/9 9:56 AM
 */
public class LockConfigurationBuilder {


    private int tenancySeconds = 30;
    private int lockCompetitionParkInterval = 1000;
    private boolean throwExceptionWhenUnlockTimeout = false;
    private boolean unlockWhenClose = true;
    private String lockPrefix = "RedisLock_";

    public LockConfigurationBuilder tenancySeconds(int tenancySeconds) {
        this.tenancySeconds = tenancySeconds;
        return this;
    }

    public LockConfigurationBuilder lockCompetitionParkInterval(int lockCompetitionParkInterval) {
        this.lockCompetitionParkInterval = lockCompetitionParkInterval;
        return this;
    }

    public LockConfigurationBuilder throwExceptionWhenUnlockTimeout(boolean throwExceptionWhenUnlockTimeout) {
        this.throwExceptionWhenUnlockTimeout = throwExceptionWhenUnlockTimeout;
        return this;
    }

    public LockConfigurationBuilder unlockWhenClose(boolean unlockWhenClose) {
        this.unlockWhenClose = unlockWhenClose;
        return this;
    }

    public LockConfigurationBuilder lockPrefix(String lockPrefix) {
        this.lockPrefix = lockPrefix;
        return this;
    }

    public LockConfiguration build() {
        return new LockConfiguration(tenancySeconds, lockCompetitionParkInterval, throwExceptionWhenUnlockTimeout,
                unlockWhenClose, lockPrefix);
    }

}
