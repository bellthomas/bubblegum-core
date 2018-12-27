package io.hbt.bubblegum.core.kademlia.activities;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.strands.SuspendableRunnable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ActivityExecutionContext {

//    private final ActivityExecutionManager activityManager;
//    private final ActivityExecutionManager callbackManager;
//    private final ActivityExecutionManager pingManager;

    protected final static int MAX_THREADS_IN_CONTEXT = 200000;

    protected final static int GENERAL_ACTIVITY_PARALLELISM = 5;
    protected final static int CALLBACK_ACTIVITY_PARALLELISM = 10;
    protected final static int PING_ACTIVITY_PARALLELISM = 5;

    private int numProcesses = 0;

    private final ScheduledExecutorService executor;

    public ActivityExecutionContext(int numProcesses) {
        this.numProcesses = (numProcesses < 0) ? 0 : numProcesses;
        if(numProcesses < 1) numProcesses = 1;

        double parallelismTotal = GENERAL_ACTIVITY_PARALLELISM + CALLBACK_ACTIVITY_PARALLELISM + PING_ACTIVITY_PARALLELISM;

        executor = Executors.newScheduledThreadPool(20);

//        this.activityManager = new ActivityExecutionManager(
//            numProcesses, GENERAL_ACTIVITY_PARALLELISM,
//            (int)((GENERAL_ACTIVITY_PARALLELISM / parallelismTotal) * MAX_THREADS_IN_CONTEXT)
//        );
//
//        this.callbackManager = new ActivityExecutionManager(
//            numProcesses, CALLBACK_ACTIVITY_PARALLELISM,
//            (int)((CALLBACK_ACTIVITY_PARALLELISM / parallelismTotal) * MAX_THREADS_IN_CONTEXT)
//        );
//
//        this.pingManager = new ActivityExecutionManager(
//            numProcesses,
//            PING_ACTIVITY_PARALLELISM,
//            (int)((PING_ACTIVITY_PARALLELISM / parallelismTotal) * MAX_THREADS_IN_CONTEXT)
//        );
    }

    public void addActivity(String owner, SuspendableRunnable r) {
        new Fiber<>(() -> r.run()).start();
//        this.activityManager.addActivity(owner, r);
    }

    public void addPingActivity(String owner, SuspendableRunnable r) {
        new Fiber<>(() -> r.run()).start();
//        this.pingManager.addActivity(owner, r);
    }

    public void addCallbackActivity(String owner, SuspendableRunnable r) {
        new Fiber<>(() -> r.run()).start();
//        this.callbackManager.addActivity(owner, r);
    }

    public void addDelayedActivity(String owner, SuspendableRunnable r, long milliseconds) {
//        this.activityManager.addDelayedActivity(owner, r, milliseconds);
    }

    public void newProcessInContext() {
//        this.numProcesses++;
//        if(this.numProcesses > 1) {
//            this.activityManager.increaseForNewProcess();
//            this.callbackManager.increaseForNewProcess();
//            this.pingManager.increaseForNewProcess();
//        }
//        System.out.println("[ActivityExecutionContext] Now setup for " + this.numProcesses + " processes.");
    }

    public void newProcessesInContext(int numProcesses) {
//        if(this.numProcesses == 0) {
//            this.numProcesses += numProcesses;
//            numProcesses--;
//        }
//        else {
//            this.numProcesses += numProcesses;
//        }

//        this.activityManager.increaseForNewProcesses(numProcesses);
//        this.callbackManager.increaseForNewProcesses(numProcesses);
//        this.pingManager.increaseForNewProcesses(numProcesses);
//        System.out.println("[ActivityExecutionContext] Now setup for " + this.numProcesses + " processes.");
    }

    public void scheduleTask(Runnable command, long initial, long period, TimeUnit unit) {
        this.executor.scheduleAtFixedRate(command, initial, period, unit);
    }
}

/**


 minimum = numNetworks * parallelism
 maximum = 2000


 */