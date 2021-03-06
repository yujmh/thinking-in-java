package concurrency.exercise;

import java.util.Random;
import java.util.concurrent.*;

import static net.mindview.util.Print.print;

/**
 * @author Cheng Cheng
 * @date 2018-08-03 19:13
 */

class Toast {

    public enum Status {
        DRY,
        BUTTERED,
        JAMMED,
        READY {
            @Override
            public String toString() {
                return BUTTERED.toString() + " & " + JAMMED.toString();
            }
        }
    }

    private Status status = Status.DRY;

    private final int id;

    public Toast(int id) {
        this.id = id;
    }

    public void butter() {
        status = (status == Status.DRY) ? Status.BUTTERED : Status.READY;
    }

    public void jam() {
        status = (status == Status.DRY) ? Status.JAMMED : Status.READY;
    }

    public Status getStatus() {
        return status;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Toast " + id + ":" + status;
    }
}

class ToastQueue extends LinkedBlockingQueue<Toast> {
}

class Toaster implements Runnable {
    private ToastQueue toastQueue;
    private int count = 0;
    private Random rand = new Random(47);

    public Toaster(ToastQueue dry) {
        toastQueue = dry;
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void run() {
        try {
            while (!Thread.interrupted()) {
                TimeUnit.MILLISECONDS.sleep(100 + rand.nextInt(500));
                // Make toast
                Toast t = new Toast(count++);
                print(t);
                // Insert into queue
                toastQueue.put(t);
            }
        } catch (InterruptedException e) {
            print("Toaster interrupted");
        }
        print("Toaster off");
    }
}

// Apply butter to toast:
class Butterer implements Runnable {
    private ToastQueue inQueue, butteredQueue;

    public Butterer(ToastQueue in, ToastQueue buttered) {
        inQueue = in;
        butteredQueue = buttered;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                // Blocks until next piece of toast is available:
                Toast t = inQueue.take();
                t.butter();
                print(t);
                butteredQueue.put(t);
            }
        } catch (InterruptedException e) {
            print("Buffer interrupted");
        }
        print("Buffer off");
    }
}

// Apply jam to toast:
class Jammer implements Runnable {
    private ToastQueue inQueue, jammedQueue;

    public Jammer(ToastQueue in, ToastQueue jammed) {
        inQueue = in;
        jammedQueue = jammed;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                // Blocks until next piece of toast is available:
                Toast t = inQueue.take();
                t.jam();
                print(t);
                jammedQueue.put(t);
            }
        } catch (InterruptedException e) {
            print("Jammer interrupted");
        }
        print("Jammer off");
    }
}

// Consume the toast:
class Eater implements Runnable {
    private ToastQueue finishedQueue;

    public Eater(ToastQueue finished) {
        finishedQueue = finished;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                // Blocks until next piece of toast is available:
                Toast t = finishedQueue.take();
                // Verify that all pieces are ready for consumption:
                if (t.getStatus() != Toast.Status.READY) {
                    print(">>>> Error: " + t);
                    System.exit(1);
                } else
                    print("Chomp! " + t);
            }
        } catch (InterruptedException e) {
            print("Eater interrupted");
        }
        print("Eater off");
    }
}

// Outputs alternate inputs on alternate channels:
class Alternator implements Runnable {
    private ToastQueue inQueue, out1Queue, out2Queue;
    private boolean outTo2; // control alternation

    public Alternator(ToastQueue in, ToastQueue out1, ToastQueue out2) {
        inQueue = in;
        out1Queue = out1;
        out2Queue = out2;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                // Blocks until next piece of toast is available:
                Toast t = inQueue.take();
                if (!outTo2) {
                    out1Queue.put(t);
                } else {
                    out2Queue.put(t);
                }
                outTo2 = !outTo2; // change state for next time
            }
        } catch (InterruptedException e) {
            print("Alternator interrupted");
        }
        print("Alternator off");
    }
}

// Accepts toasts on either channel, and relays them on to
// a "single" successor
class Merger implements Runnable {
    private ToastQueue
            in1Queue, in2Queue, toBeButteredQueue,
            toBeJammedQueue, finishedQueue;

    public Merger(ToastQueue in1, ToastQueue in2,
                  ToastQueue toBeButtered, ToastQueue toBeJammed,
                  ToastQueue finished) {
        in1Queue = in1;
        in2Queue = in2;
        toBeButteredQueue = toBeButtered;
        toBeJammedQueue = toBeJammed;
        finishedQueue = finished;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                // Blocks until next piece of toast is available:
                Toast t = null;
                while (t == null) {
                    t = in1Queue.poll(50, TimeUnit.MILLISECONDS);
                    if (t != null) {
                        break;
                    }
                    t = in2Queue.poll(50, TimeUnit.MILLISECONDS);
                }
                // Relay toast onto the proper queue
                switch (t.getStatus()) {
                    case BUTTERED:
                        toBeButteredQueue.put(t);
                        break;
                    case JAMMED:
                        toBeJammedQueue.put(t);
                        break;
                    default:
                        finishedQueue.put(t);
                }
            }
        } catch (InterruptedException e) {
            print("Merger interrupted");
        }
        print("Merger off");
    }
}

public class E29_ToastOMatic2 {
    public static void main(String[] args) throws InterruptedException {
        ToastQueue
                dryQueue = new ToastQueue(),
                butteredQueue = new ToastQueue(),
                toBeButteredQueue = new ToastQueue(),
                jammedQueue = new ToastQueue(),
                toBeJammedQueue = new ToastQueue(),
                finishedQueue = new ToastQueue();
        ExecutorService exec = Executors.newCachedThreadPool();
        exec.execute(new Toaster(dryQueue));
        exec.execute(new Alternator(dryQueue, toBeButteredQueue, toBeJammedQueue));
        exec.execute(new Butterer(toBeButteredQueue, butteredQueue));
        exec.execute(new Jammer(toBeJammedQueue, jammedQueue));
        exec.execute(new Merger(butteredQueue, jammedQueue, toBeButteredQueue, toBeJammedQueue, finishedQueue));
        exec.execute(new Eater(finishedQueue));

        TimeUnit.SECONDS.sleep(5);
        exec.shutdownNow();
    }
}
