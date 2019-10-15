import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 1. park parkNanos 不会释放 ReentrantLock
 * 2. park parkNanos 不会释放 synchronized
 * 3. park 对应 WAITING；parkNanos 对应 TIMED_WAITING
 * <p>
 * <p>
 * 4. 线程等待 synchronized 时，状态为BLOCKED
 * 5. 线程等待 ReentrantLock 时，状态为WAITING
 *
 * @author Brozen
 * @date 2019/5/21 4:11 PM
 */
public class TestThreadStatus {

    private static final Object LOCK = new Object();

    public static void main(String[] args) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        AtomicReference<Condition> condition = new AtomicReference<>();

        Thread t1 = new Thread(() -> {
            System.out.println("3:" + Thread.currentThread().getState());

            /*synchronized (LOCK) {
                System.out.println("3:in synchronized");
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000));
                LockSupport.park();
            }

            System.out.println("3:out of synchronized");*/

            lock.lock();
            condition.set(lock.newCondition());
            try {
                condition.get().await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
            System.out.println("4:" + Thread.currentThread().getState());
        });

        System.out.println("1:" + t1.getState());
        lock.lock();
        System.out.println("1:in lock");
        t1.start();
        Thread.sleep(100);
        System.out.println("1:" + t1.getState());
        lock.unlock();

       /* synchronized (LOCK) {
            System.out.println("1: in synchronized");
            Thread.sleep(100);
            System.out.println("1:" + t1.getState());
        }*/
        System.out.println("1:" + t1.getState());
        Thread.sleep(500L);

        System.out.println("1 lock:" + lock.tryLock());

        System.out.println("1:" + t1.getState());
        Thread.sleep(700L);
        System.out.println("1:" + t1.getState());
    }

}
