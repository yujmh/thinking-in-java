package concurrency.exercise;

import java.util.concurrent.TimeUnit;

import static net.mindview.util.Print.printnb;

class Daemon2 implements Runnable {
    private Thread[] t = new Thread[10];

    public void run() {
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(new DaemonSpawn());
            t[i].start();
            printnb("DaemonSpawn " + i + " started, ");
        }
        try {
            // To better see the effect of altering main
            // application thread's sleep time.
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) { /* Ignore */ }
        for (int i = 0; i < t.length; i++) {
            printnb("t[" + i + "].isDaemon() = " + t[i].isDaemon() + ", ");
        }
        while (true) {
            Thread.yield();
        }
    }
}

class DaemonSpawn implements Runnable {
    @Override
    public void run() {
        while (true) {
            Thread.yield();
        }
    }
}

public class E07_Daemons2 {
    public static void main(String[] args) throws InterruptedException {
        int sleep_time = 10;
        Thread d = new Thread(new Daemon2());
        d.setDaemon(true);
        d.start();
        printnb("d.isDaemon() = " + d.isDaemon() + ", ");
        TimeUnit.MILLISECONDS.sleep(sleep_time);
    }
}
