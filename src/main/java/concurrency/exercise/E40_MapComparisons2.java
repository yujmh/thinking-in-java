package concurrency.exercise;

import net.mindview.util.CountingGenerator;
import net.mindview.util.Generator;
import net.mindview.util.MapData;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class ReaderWriterMap<K, V> extends AbstractMap<K, V> {
    private HashMap<K, V> lockedMap;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public ReaderWriterMap(Generator<K> genK, int size, V initialValue) {
        lockedMap = new HashMap<K, V>(MapData.map(genK, initialValue, size));
    }

    public V put(K key, V value) {
        Lock wlock = lock.writeLock();
        wlock.lock();
        try {
            return lockedMap.put(key, value);
        } finally {
            wlock.unlock();
        }
    }

    public V get(Object key) {
        Lock rlock = lock.readLock();
        rlock.lock();
        try {
            // Comment out if you would like to trace how many
            // readers are acquiring the lock simultaneously:
//            if (lock.getReadLockCount() > 1) {
//                print(lock.getReadLockCount());
//            }
            return lockedMap.get(key);
        } finally {
            rlock.unlock();
        }
    }

    // Dummy implementation
    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }
}

class ReaderWriterMapTest extends MapTest {
    Map<Integer, Integer> containerInitializer() {
        return new ReaderWriterMap<Integer, Integer>(
                new CountingGenerator.Integer(), containerSize, 1);
    }

    ReaderWriterMapTest(int nReaders, int nWriters) {
        super("ReaderWriterMap", nReaders, nWriters);
    }
}

public class E40_MapComparisons2 {
    public static void main(String[] args) {
        Tester.initMain(args);
        new SynchronizedHashMapTest(10, 0);
        new SynchronizedHashMapTest(9, 1);
        new SynchronizedHashMapTest(5, 5);
        new ConcurrentHashMapTest(10, 0);
        new ConcurrentHashMapTest(9, 1);
        new ConcurrentHashMapTest(5, 5);
        new ReaderWriterMapTest(10, 0);
        new ReaderWriterMapTest(9, 1);
        new ReaderWriterMapTest(5, 5);
        Thread.yield();
        Tester.exec.shutdown();
    }
}
