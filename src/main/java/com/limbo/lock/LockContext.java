package com.limbo.lock;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Brozen
 * @date 2019/5/8 3:04 PM
 */
@Slf4j
public class LockContext {
    // 解锁Lua脚本
    static final String UNLOCK_SCRIPT;

    static {
        String unlockScript;
        try (InputStream lockScriptStream = ClassLoader.getSystemResourceAsStream("com/limbo/lock/unlock.lua")) {
            unlockScript = IOUtils.toString(lockScriptStream, "UTF-8");
        } catch (IOException e) {
            unlockScript = null;
        }
        UNLOCK_SCRIPT = unlockScript;
    }

    private LockConfiguration defaultConfiguration;

    // 锁上下文唯一ID，可作为标识锁的占有者的前缀
    @Getter
    private String lockContextUID;

    private RedisConnectionFactory connectionFactory;

    // 用于向Redis发送消息
    @Getter
    private RedisConnection connection;

    // 用于从Redis监听事件，connection用于监听事件后将无法再发送消息
    private RedisConnection subscribeConnection;

    @Getter
    private Observable<RedisMessageInfo> messageObservable;

    public LockContext(LockConfiguration defaultConfiguration, RedisConnectionFactory connectionFactory) {
        this.defaultConfiguration = defaultConfiguration;
        this.connectionFactory = connectionFactory;
        this.lockContextUID = Utils.getMac() + "__" + Utils.getPID() + "__" + hashCode();

        this.connection = connectionFactory.getConnection();
        this.subscribeConnection = connectionFactory.getConnection();
        this.messageObservable = Observable.<RedisMessageInfo>create(emitter -> {
            this.subscribeConnection.pSubscribe((message, pattern) -> {
                String msgBody = message.toString();
                String msgChannel = new String(message.getChannel());
                // 判断事件类型
                RedisMessage redisMessage = RedisMessage.parse(msgBody);
                if (redisMessage == null) {
                    log.debug("不处理的Redis事件：{}", msgBody);
                    return;
                }
                // 发射事件
                String key = msgChannel.split(RedisLock.LOCK_NAME_PREFIX_SPLITER)[1];
                emitter.onNext(new RedisMessageInfo(key, redisMessage));
            }, ("__keyspace@0__:" + defaultConfiguration.getLockPrefix() + "*").getBytes());
        }).subscribeOn(Schedulers.io()).observeOn(Schedulers.io());
    }

    public RedisLock createLock(String lockName, LockConfiguration configuration) {
        return new RedisLock(this, lockName, configuration);
    }

    public RedisLock createLock(String lockName) {
        return new RedisLock(this, lockName, defaultConfiguration);
    }

    protected void disposeLock(RedisLock lock) {
        if (lock == null) {
            return;
        }

        if (lock.messageDisposable != null && !lock.messageDisposable.isDisposed()) {
            lock.messageDisposable.dispose();
        }
    }

    @Data
    public static class RedisMessageInfo {
        private String key;
        private RedisMessage message;

        RedisMessageInfo(String key, RedisMessage message) {
            this.key = key;
            this.message = message;
        }
    }

}
