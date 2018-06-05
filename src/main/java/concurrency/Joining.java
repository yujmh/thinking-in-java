package concurrency;

import static net.mindview.util.Print.print;

/**
 * @author Cheng Cheng
 * @date 2018-04-18 16:31
 */
public class Joining {
    public static void main(String[] args) {
        Sleeper
//                sleeper = new Sleeper("Sleepy", 1500),
                grumpy = new Sleeper("Grumpy", 15000);
        Joiner
//                dopey = new Joiner("Dopey", sleeper),
                doc = new Joiner("Doc", grumpy);
//        grumpy.interrupt();
    }
}


class Sleeper extends Thread {
    private int duration;

    public Sleeper(String name, int sleepTime) {
        super(name);
        this.duration = sleepTime;
        start();
    }

    @Override
    public void run() {
        try {
            sleep(duration);
        } catch (InterruptedException e) {
            print(getName() + " was interrupted. isInterrupted(): " + isInterrupted());
            return;
        }
        print(getName() + " has awakened");
    }
}

class Joiner extends Thread {
    private Sleeper sleeper;

    public Joiner(String name, Sleeper sleeper) {
        super(name);
        this.sleeper = sleeper;
        start();
    }

    @Override
    public void run() {
        try {
            sleeper.join();
        } catch (InterruptedException e) {
            print("Interrupted");
        }
        print(getName() + " join completed");
    }
}