package concurrency.exercise;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static net.mindview.util.Print.printf;

public class E39_FastSimulation2 {
    static final int N_ELEMENTS = 100000;
    static final int N_GENES = 30;
    static final int N_EVOLVES = 50;
    // variant 1 (optimistic locking)
    static final AtomicInteger[][] GRID = new AtomicInteger[N_ELEMENTS][N_GENES];
    // variant 2 (explicit locking)
    static final int[][] grid = new int[N_ELEMENTS][N_GENES];
    static final ReentrantLock[] lock = new ReentrantLock[N_ELEMENTS];

    // Counts the number of evolutions using 'variant 1':
    static final AtomicInteger counter1 = new AtomicInteger();
    // Counts the number of evolutions using 'variant 2':
    static final AtomicInteger counter2 = new AtomicInteger();

    static Random rand = new Random(47);

    static class Evolver1 implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                // Randomly select an element to work on:
                int element = rand.nextInt(N_ELEMENTS);
                for (int i = 0; i < N_GENES; i++) {
                    int previous = element - 1;
                    int next = element + 1;
                    if (previous < 0) previous = N_ELEMENTS - 1;
                    if (next >= N_ELEMENTS) next = 0;
                    int oldvalue = GRID[element][i].get();
                    int newvalue = oldvalue + GRID[previous][i].get() + GRID[next][i].get();
                    newvalue /= 3;
                    if (!GRID[element][i].compareAndSet(oldvalue, newvalue)) {
                        // Some backup action...
                    }
                }
                counter1.incrementAndGet();
            }
        }
    }

    static class Evolver2 implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                // Randomly select an element to work on:
                int element = rand.nextInt(N_ELEMENTS);
                // Lock the whole row:
                lock[element].lock();
                try {
                    for (int i = 0; i < N_GENES; i++) {
                        int previous = element - 1;
                        int next = element + 1;
                        if (previous < 0) previous = N_ELEMENTS - 1;
                        if (next >= N_ELEMENTS) next = 0;
                        int newvalue = grid[element][i] + grid[previous][i] + grid[next][i];
                        newvalue /= 3;
                        grid[element][i] = newvalue;
                    }
                } finally {
                    lock[element].unlock();
                }
                counter2.incrementAndGet();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ExecutorService exec = Executors.newCachedThreadPool();
        for (int i = 0; i < N_ELEMENTS; i++) {
            for (int j = 0; j < N_GENES; j++) {
                GRID[i][j] = new AtomicInteger(rand.nextInt(1000));
            }
        }

        for (int i = 0; i < N_ELEMENTS; i++) {
            for (int j = 0; j < N_GENES; j++) {
                grid[i][j] = rand.nextInt(1000);
            }
        }

        for (int i = 0; i < N_ELEMENTS; i++) {
            lock[i] = new ReentrantLock();
        }

        for (int i = 0; i < N_EVOLVES; i++) {
            exec.execute(new Evolver1());
            exec.execute(new Evolver2());
        }

        // 休眠5s，观察计算器的大小，推断两种加锁方式的性能开销
        TimeUnit.SECONDS.sleep(5);
        exec.shutdownNow();
        printf("Variant 1: %d\n", counter1.get());
        printf("Variant 2: %d\n", counter2.get());
        // 试验不同的锁定方案；例如：锁定比整个行小的对象。只有当失败的成本很低时，风险乐观锁定。
        // 否则，您首先就会失去避免显式锁所获得的优势。
    }
}
