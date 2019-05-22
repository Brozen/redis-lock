package com.limbo.lock.annotations;

import com.limbo.lock.aop.LockPolicy;

import java.lang.annotation.*;

/**
 * @author Brozen
 * @date 2019/5/21 7:02 PM
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Locked {

    /**
     * 锁名称；
     * 若此处不指定锁名，可以使用expression指定生成锁名称的SpEL表达式；
     */
    String lockName();

    /**
     * 生成锁名称的SpELl表达式，如果指定了LockName，则该表达式无效；
     */
    String expression();

    /**
     * 加锁策略：阻塞锁、自旋锁、快速失败锁；
     */
    LockPolicy policy() default LockPolicy.BLOCK;

    /**
     * 加锁策略选择自旋时，自旋超时时间，超过指定毫秒数未获取到锁则抛出异常；
     */
    long spinMillis() default 1000;

}
