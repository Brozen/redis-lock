import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Brozen
 * @date 2019/5/21 4:11 PM
 */
public class TestThreadStatus {

    public static void main(String[] args) {
        ReentrantLock lock = new ReentrantLock();
        AtomicReference<Condition> condition = new AtomicReference<>();

        Thread t1 = new Thread(() -> {
            System.out.println("3:" + Thread.currentThread().getState());
            // Thread.sleep(100L);
            // LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
            // LockSupport.park();
            /*synchronized (lock) {
                lock.wait();
            }*/
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
        t1.start();

        long st = System.currentTimeMillis();
        while (t1.getState() != Thread.State.TERMINATED) {
            System.out.println("2:" + t1.getState());
            if (System.currentTimeMillis() - st > 10) {
                // LockSupport.unpark(t1);
                if (condition.get() != null) {
                    lock.lock();
                    condition.get().signal();
                    System.out.println("5:" + t1.getState());
                    lock.unlock();
                }
            }
        }
    }

}
