package com.limbo.lock;

/**
 * @author Brozen
 * @date 2019/5/8 4:15 PM
 */
public enum RedisMessage {

    EXPIRED("expired"),
    EVICT("evict"),
    DELETE("del"),
    ;


    RedisMessage(String messageContent) {
        this.messageContent = messageContent;
    }

    private String messageContent;


    public boolean is (String msg) {
        return this.messageContent.equals(msg);
    }

    public static RedisMessage parse(String msg) {
        for (RedisMessage redisMessage : RedisMessage.values()) {
            if (redisMessage.is(msg)) {
                return redisMessage;
            }
        }
        return null;
    }
}
