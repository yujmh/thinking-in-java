package concurrency.exercise;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.*;

import static net.mindview.util.Print.print;
import static net.mindview.util.Print.printnb;

class WebClient {
    private final int serverTime;

    public WebClient(int serverTime) {
        this.serverTime = serverTime;
    }

    public int getServerTime() {
        return serverTime;
    }

    @Override
    public String toString() {
        return "[" + serverTime + "]";
    }
}

class WebClientLine extends ArrayBlockingQueue<WebClient> {
    public WebClientLine(int maxLineSize) {
        super(maxLineSize);
    }

    @Override
    public String toString() {
        if (this.size() == 0) {
            return "[Empty]";
        }
        StringBuilder result = new StringBuilder();
        for (WebClient client : this) {
            result.append(client);
        }
        return result.toString();
    }
}

class WebClientGenerator implements Runnable {
    private WebClientLine clients;
    volatile int loadFactor = 1; // Start with one client/sec
    private static Random rand = new Random(47);

    public WebClientGenerator(WebClientLine clients) {
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                clients.put(new WebClient(rand.nextInt(1000)));
                TimeUnit.MILLISECONDS.sleep(1000 / loadFactor);
            }
        } catch (InterruptedException e) {
            System.out.println("WebClientGenerator interrupted");
        }
        System.out.println("WebClientGenerator terminating");
    }
}

class Server implements Runnable {
    private static int counter = 0;
    private final int id = counter++;
    private WebClientLine clients;

    public Server(WebClientLine clients) {
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                WebClient client = clients.take();
                TimeUnit.MILLISECONDS.sleep(client.getServerTime());
            }
        } catch (InterruptedException e) {
            System.out.println(this + " interrupted");
        }
        System.out.println(this + " terminating");
    }

    @Override
    public String toString() {
        return "Server " + id;
    }

    public String shortString() {
        return "T" + id;
    }
}

class SimulationManager implements Runnable {
    private ExecutorService exec;
    private WebClientGenerator gen;
    private WebClientLine clients;
    private Queue<Server> servers = new LinkedList<>();
    private int adjustmentPeriod;

    // Indicates whether the queue is stable
    private boolean stable = true;
    private int prevSize;

    public SimulationManager(ExecutorService e, WebClientGenerator gen, WebClientLine clients, int adjustmentPeriod, int n) {
        exec = e;
        this.gen = gen;
        this.clients = clients;
        this.adjustmentPeriod = adjustmentPeriod;
        // Start with 'n' servers:
        for (int i = 0; i < n; i++) {
            Server server = new Server(clients);
            exec.execute(server);
            servers.add(server);
        }
    }

    public void adjustLoadFactor() {
        // This is actually a control system. By adjusting
        // the numbers, you can reveal stability issues in
        // the control mechanism.
        // If line is stable, increase the 'load factor':
        if (clients.size() > prevSize) {
            if (stable) { // Was stable previous time:
                stable = false;
            } else if (!stable) { // Not stable for a second time
                print("Peak load factor: ~" + gen.loadFactor);
                exec.shutdownNow();
            }
        } else {
            print("New load factor: " + ++gen.loadFactor);
            stable = true;
        }
        prevSize = clients.size();
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                TimeUnit.MILLISECONDS.sleep(adjustmentPeriod);
                System.out.print(clients + "{");
                for (Server server : servers) {
                    printnb(server.shortString() + " ");
                }
                print("}");
                adjustLoadFactor();
            }
        } catch (InterruptedException e) {
            System.out.println(this + "interrupted");
        }
        System.out.println(this + "terminating");
    }

    @Override
    public String toString() {
        return "SimulationManager ";
    }
}

public class E35_WebClientServerSimulation {
    static final int MAX_LINE_SIZE = 50;
    static final int NUM_OF_SERVERS = 3;
    static final int ADJUSTMENT_PERIOD = 1000;

    public static void main(String[] args) throws InterruptedException, IOException {
        ExecutorService exec = Executors.newCachedThreadPool();
        WebClientLine clients = new WebClientLine(MAX_LINE_SIZE);
        WebClientGenerator g = new WebClientGenerator(clients);
        exec.execute(g);
        exec.execute(new SimulationManager(exec, g, clients, ADJUSTMENT_PERIOD, NUM_OF_SERVERS));

        if (args.length > 0) { // Optional argument
            TimeUnit.SECONDS.sleep(new Integer(args[0]));
        } else {
            System.out.println("Press 'Enter' to quit");
            System.in.read();
        }
        exec.shutdownNow();
    }
}
