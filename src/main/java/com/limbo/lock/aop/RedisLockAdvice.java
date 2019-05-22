package com.limbo.lock.aop;

import com.limbo.lock.RedisLockContext;
import com.limbo.lock.RedisLock;
import com.limbo.lock.annotations.Locked;
import com.limbo.lock.utils.Ref;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Brozen
 * @date 2019/5/21 6:54 PM
 */
@Slf4j
@Aspect
@Component
public class RedisLockAdvice implements DisposableBean {

    private static final Map<Class<?>, Map<String, Method>> LOCKED_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Expression> LOCK_NAME_EXPRESSION_CACHE = new ConcurrentHashMap<>();

    private RedisLockContext lockContext;
    private SpelExpressionParser parser;

    @Autowired
    public RedisLockAdvice(RedisLockContext redisLockContext) {
        this.lockContext = redisLockContext;
        this.parser = new SpelExpressionParser();
    }

    @Around(value = "@annotation(locked)", argNames = "joinPoint, locked")
    public Object doWithLock(ProceedingJoinPoint joinPoint, Locked locked) throws Throwable {

        Class<?> clazz = joinPoint.getTarget().getClass();
        Signature signature = joinPoint.getSignature();
        Method method = getCachedMethod(clazz, signature);

        try (RedisLock redisLock = createLock(method, joinPoint.getArgs(), locked)) {
            LockPolicy policy = locked.policy();
            Objects.requireNonNull(policy, "加锁策略不可为空！");

            Ref<Object> resultRef = new Ref<>();
            Ref<Throwable> exceptionRef = new Ref<>();
            policy.doInLock(redisLock, locked, () -> {
                try {
                    resultRef.value = joinPoint.proceed();
                } catch (Throwable throwable) {
                    exceptionRef.value = throwable;
                }
            });

            // 如果执行期间抛出了异常，则此处也应该抛出去
            if (exceptionRef.value != null) {
                throw exceptionRef.value;
            }
            return resultRef.value;
        }
    }

    /**
     * 创建一个锁，根据lockName或expression，优先使用lockName
     */
    private RedisLock createLock(Method method, Object[] methodArgs, Locked locked) {
        String lockName = locked.lockName();
        String lockNameExpression = locked.expression();
        if (!StringUtils.hasText(lockName) && !StringUtils.hasText(lockNameExpression)) {
            return null;
        }

        if (!StringUtils.hasText(lockName)) {
            // 未指定LockName，SpEL表达式计算LockName
            lockName = calculateLockNameExpression(method, methodArgs, lockNameExpression);
        }

        return lockContext.createLock(lockName);
    }

    /**
     * 根据expression计算锁名称，基于SpEL表达式
     */
    private String calculateLockNameExpression(Method method, Object[] methodArgs, String expressionStr) {
        Expression expression = LOCK_NAME_EXPRESSION_CACHE.get(expressionStr);
        if (expression == null) {
            expression = parser.parseExpression(expressionStr);
            LOCK_NAME_EXPRESSION_CACHE.put(expressionStr, expression);
        }

        return expression.getValue(createEvaluationContext(method, methodArgs), String.class);
    }

    /**
     * 创建SpEL表达式上下文
     */
    private StandardEvaluationContext createEvaluationContext(Method method, Object[] methodArgs) {
        LocalVariableTableParameterNameDiscoverer methodArgParser = new LocalVariableTableParameterNameDiscoverer();
        StandardEvaluationContext spelContext = new StandardEvaluationContext();
        String [] argNames = methodArgParser.getParameterNames(method);
        for (int i = 0; i < argNames.length; i++) {
            spelContext.setVariable(argNames[i], methodArgs[i]);
        }
        return spelContext;
    }

    /**
     * 获取缓存的Method对象
     */
    private Method getCachedMethod(Class<?> clazz, Signature signature) {
        String methodName = signature.getName();
        LOCKED_METHOD_CACHE.putIfAbsent(clazz, new ConcurrentHashMap<>());
        Map<String, Method> methodCache = LOCKED_METHOD_CACHE.get(clazz);
        if (!methodCache.containsKey(methodName)) {
            Method method;
            if (signature instanceof MethodSignature) {
                method = ((MethodSignature) signature).getMethod();
            } else {
                method = ClassUtils.getMethod(clazz, methodName);
            }

            if (method == null) {
                log.error("无法在切面中获取到调用的方法:{}, {}", clazz.getName(), methodName);
                throw new IllegalStateException("无法在切面中获取到调用的方法:" + clazz.getName() + ", " + signature);
            } else {
                methodCache.putIfAbsent(methodName, method);
            }
            return method;
        }

        return methodCache.get(methodName);
    }

    @Override
    public void destroy() throws Exception {
        lockContext.close();
    }
}
