package concurrency.exercise;

/*
 * 练习37：
 * (2)修改<code>CarBuilder</code> ，在汽车构建过程中添加一个阶段，即添加排气系统、车身和保险杠。
 *    与第二个阶段相同，假设这些处理可以由机器人同时执行。
 */

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static net.mindview.util.Print.print;

class Car2 {
    private final int id;
    private boolean
            engine = false, driveTrain = false, wheels = false,
            exhaustSystem = false, body = false, fender = false;

    public Car2(int idn) {
        id = idn;
    }

    public Car2() {
        id = -1;
    }

    public synchronized int getId() {
        return id;
    }

    public synchronized void addEngine() {
        // 发动机
        engine = true;
    }

    public synchronized void addDriveTrain() {
        // 车厢
        driveTrain = true;
    }

    public synchronized void addWheels() {
        // 轮子
        wheels = true;
    }

    public synchronized void addExhaustSystem() {
        // 排气系统
        exhaustSystem = true;
    }

    public synchronized void addBody() {
        // 车身
        body = true;
    }

    public synchronized void addFender() {
        // 保险杠
        fender = true;
    }

    @Override
    public synchronized String toString() {
        return "Car " + id
                + " ["
                + " engine: " + engine + " driveTrain: " + driveTrain + " wheels: " + wheels
                + " exhaustSystem: " + exhaustSystem + " body: " + body + " fender: " + fender
                + " ]";
    }
}

class CarQueue extends LinkedBlockingQueue<Car2> {
}

class ChassisBuilder implements Runnable {
    private CarQueue carQueue;
    private int counter = 0;

    public ChassisBuilder(CarQueue carQueue) {
        this.carQueue = carQueue;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                TimeUnit.MILLISECONDS.sleep(500);
                // Make chassis:
                Car2 c = new Car2(counter++);
                print("ChassisBuilder created " + c);
                // Insert into queue
                carQueue.put(c);
            }
        } catch (InterruptedException e) {
            print("Interrupted: ChassisBuilder");
        }
        print("ChassisBuilder off");
    }
}

class Assembler implements Runnable {
    private CarQueue chassisQueue, finishingQueue;
    private Car2 car;
    private CyclicBarrier barrier = new CyclicBarrier(4);
    private RobotPool robotPool;

    public Assembler(CarQueue cq, CarQueue fq, RobotPool rp) {
        chassisQueue = cq;
        finishingQueue = fq;
        robotPool = rp;
    }

    public Car2 car() {
        return car;
    }

    public CyclicBarrier barrier() {
        return barrier;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                // Blocks until chassis is available:
                car = chassisQueue.take();
                // Hire robots to perform work (second stage):
                robotPool.hire(EngineRobot.class, this);
                robotPool.hire(DriveTrainRobot.class, this);
                robotPool.hire(WheelRobot.class, this);
                barrier.await();
                // Hire robots to perform work (third stage):
                robotPool.hire(ExhaustSystemRobot.class, this);
                robotPool.hire(BodyRobot.class, this);
                robotPool.hire(FenderRobot.class, this);
                barrier.await();
                // Put car into finishingQueue for further work
                finishingQueue.put(car);
            }
        } catch (InterruptedException e) {
            print("Exiting Assembler via interrupt");
        } catch (BrokenBarrierException e) {
            // This one we want to know about
            throw new RuntimeException(e);
        }
        print("Assembler off");
    }
}

class Reporter implements Runnable {
    private CarQueue carQueue;

    public Reporter(CarQueue carQueue) {
        this.carQueue = carQueue;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                print(carQueue.take());
            }
        } catch (InterruptedException e) {
            print("Exiting Reporter via interrupt");
        }
        print("Reporter off");
    }
}

abstract class Robot implements Runnable {
    private RobotPool pool;
    private boolean engage = false;
    protected Assembler assembler;

    public Robot(RobotPool p) {
        pool = p;
    }

    public Robot assignAssembler(Assembler assembler) {
        this.assembler = assembler;
        return this;
    }

    public synchronized void engage() {
        engage = true;
        notifyAll();
    }

    // The part of run() that's different for each robot:
    abstract protected void performService();

    @Override
    public void run() {
        try {
            powerDown(); // Wait until needed
            while (!Thread.interrupted()) {
                performService();
                assembler.barrier().await();  // Synchronize
                // We're done with that job...
                powerDown();
            }
        } catch (InterruptedException e) {
            print("Exiting " + this + " via interrupt");
        } catch (BrokenBarrierException e) {
            // This one we want to know about
            throw new RuntimeException(e);
        }
        print(this + " off");
    }

    private synchronized void powerDown() throws InterruptedException {
        engage = false;
        assembler = null; // Disconnect from the Assembler
        // Put ourselves back in the available pool:
        pool.release(this);
        while (engage == false) // Power down
            wait();
    }

    @Override
    public String toString() {
        return getClass().getName();
    }
}

class EngineRobot extends Robot {
    public EngineRobot(RobotPool p) {
        super(p);
    }

    @Override
    protected void performService() {
        print(this + " installing Engine");
        assembler.car().addEngine();
    }
}

class DriveTrainRobot extends Robot {
    public DriveTrainRobot(RobotPool p) {
        super(p);
    }

    @Override
    protected void performService() {
        print(this + " installing DriveTrain");
        assembler.car().addDriveTrain();
    }
}

class WheelRobot extends Robot {
    public WheelRobot(RobotPool p) {
        super(p);
    }

    @Override
    protected void performService() {
        print(this + " installing Wheels");
        assembler.car().addWheels();
    }
}

class ExhaustSystemRobot extends Robot {
    public ExhaustSystemRobot(RobotPool p) {
        super(p);
    }

    @Override
    protected void performService() {
        print(this + " installing exhausting system");
        assembler.car().addExhaustSystem();
    }
}

class BodyRobot extends Robot {
    public BodyRobot(RobotPool p) {
        super(p);
    }

    @Override
    protected void performService() {
        print(this + " installing body");
        assembler.car().addBody();
    }
}

class FenderRobot extends Robot {
    public FenderRobot(RobotPool p) {
        super(p);
    }

    @Override
    protected void performService() {
        print(this + " installing fender");
        assembler.car().addFender();
    }
}

class RobotPool {
    // Quietly prevents identical entries:
    private Set<Robot> pool = new HashSet<>();

    public synchronized void add(Robot r) {
        pool.add(r);
        notifyAll();
    }

    public synchronized void hire(Class<? extends Robot> robotType, Assembler d) throws InterruptedException {
        for (Robot r : pool) {
            if (r.getClass().equals(robotType)) {
                pool.remove(r);
                r.assignAssembler(d);
                r.engage();
                return;
            }
        }
        wait(); // None available
        hire(robotType, d); // Try again, recursively
    }

    public synchronized void release(Robot r) {
        add(r);
    }
}

public class E37_CarBuilder2 {
    public static void main(String[] args) throws InterruptedException {
        CarQueue
                chassisQueue = new CarQueue(),
                finishingQueue = new CarQueue();

        ExecutorService exec = Executors.newCachedThreadPool();
        RobotPool robotPool = new RobotPool();
        exec.execute(new EngineRobot(robotPool));
        exec.execute(new DriveTrainRobot(robotPool));
        exec.execute(new WheelRobot(robotPool));
        exec.execute(new ExhaustSystemRobot(robotPool));
        exec.execute(new BodyRobot(robotPool));
        exec.execute(new FenderRobot(robotPool));
        exec.execute(new Assembler(chassisQueue, finishingQueue, robotPool));
        exec.execute(new Reporter(finishingQueue));
        // Start everything running by producing chassis:
        exec.execute(new ChassisBuilder(chassisQueue));

        TimeUnit.SECONDS.sleep(7);
        exec.shutdownNow();
    }
}
