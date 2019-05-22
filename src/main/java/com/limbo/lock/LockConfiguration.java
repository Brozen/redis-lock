package com.limbo.lock;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Brozen
 * @date 2019/5/8 3:04 PM
 */
@Data
@ConfigurationProperties(prefix = "lock")
public class LockConfiguration {

    // 锁租约时间，秒，-1表示不过期；缺省为30s
    private int tenancySeconds = 30;

    // 锁竞争时，线程暂停时长，暂停一段时间后重新进入锁竞争，单位毫秒；缺省1000；
    private int lockCompetitionParkInterval = 1000;

    // 锁租约到期尝试解锁时，失败释放抛出异常；缺省为false
    private boolean throwExceptionWhenUnlockTimeout = false;

    // 锁被销毁时，如果锁被当前虚拟机的线程占有，是否先释放锁；false则会抛出异常；缺省为true
    private boolean unlockWhenClose = true;

    // 锁名前缀，缺省为 RedisLock_；分布式环境中的多个机器中，锁前缀要一致；
    // 同一台机器中的多个LockContext锁前缀可以相同也可不同
    private String lockPrefix = "RedisLock_";

    public LockConfiguration(int tenancySeconds, int lockCompetitionParkInterval, boolean throwExceptionWhenUnlockTimeout,
                             boolean unlockWhenClose, String lockPrefix) {
        this.tenancySeconds = tenancySeconds;
        this.lockCompetitionParkInterval = lockCompetitionParkInterval;
        this.throwExceptionWhenUnlockTimeout = throwExceptionWhenUnlockTimeout;
        this.unlockWhenClose = unlockWhenClose;
        this.lockPrefix = lockPrefix;
    }

    public LockConfiguration() {
    }
}
