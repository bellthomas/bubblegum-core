package io.hbt.bubblegum.core.kademlia.activities;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ActivityExecutionContext {

    private final ActivityExecutionManager activityManager;
    private final ActivityExecutionManager callbackManager;
//    private final ActivityExecutionManager pingManager;

    protected final static int MAX_THREADS_IN_CONTEXT = 200;

    protected final static int GENERAL_ACTIVITY_PARALLELISM = 12;
    protected final static int CALLBACK_ACTIVITY_PARALLELISM = 8;
    protected final static int SHARED_PARALLELISM = 0;

    private int numProcesses = 0;

    private final ScheduledThreadPoolExecutor executor;

    public ActivityExecutionContext(int numProcesses) {
        this.numProcesses = (numProcesses < 0) ? 0 : numProcesses;
        if(numProcesses < 1) numProcesses = 1;

        double parallelismTotal = GENERAL_ACTIVITY_PARALLELISM + CALLBACK_ACTIVITY_PARALLELISM + SHARED_PARALLELISM;

        executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);

        this.activityManager = new ActivityExecutionManager(
            numProcesses, GENERAL_ACTIVITY_PARALLELISM,
            (int)((GENERAL_ACTIVITY_PARALLELISM / parallelismTotal) * MAX_THREADS_IN_CONTEXT)
        );

        this.callbackManager = new ActivityExecutionManager(
            numProcesses, CALLBACK_ACTIVITY_PARALLELISM,
            (int)((CALLBACK_ACTIVITY_PARALLELISM / parallelismTotal) * MAX_THREADS_IN_CONTEXT)
        );

//        this.pingManager = new ActivityExecutionManager(
//            numProcesses,
//            PING_ACTIVITY_PARALLELISM,
//            (int)((PING_ACTIVITY_PARALLELISM / parallelismTotal) * MAX_THREADS_IN_CONTEXT)
//        );
    }

    public void addActivity(String owner, Runnable r) {
//        new Fiber<>(() -> r.run()).start();
        this.activityManager.addActivity(owner, r);
    }

    public void addPingActivity(String owner, Runnable r) {
//        new Fiber<>(() -> r.run()).start();
        // TODO no ping?
        this.activityManager.addActivity(owner, r);
    }

    public void addCallbackActivity(String owner, Runnable r) {
//        new Fiber<>(() -> r.run()).start();
        this.callbackManager.addActivity(owner, r);
    }

    public void addDelayedActivity(String owner, Runnable r, long milliseconds) {
        this.activityManager.addDelayedActivity(owner, r, milliseconds);
    }

    public void newProcessInContext() {
        this.numProcesses++;
        if(this.numProcesses > 1) {
            this.activityManager.increaseForNewProcess();
            this.callbackManager.increaseForNewProcess();
//            this.pingManager.increaseForNewProcess();
        }
//        System.out.println("[ActivityExecutionContext] Now setup for " + this.numProcesses + " processes.");
    }

    public void newProcessesInContext(int numProcesses) {
        if(this.numProcesses == 0) {
            this.numProcesses += numProcesses;
            numProcesses--;
        }
        else {
            this.numProcesses += numProcesses;
        }

        this.activityManager.increaseForNewProcesses(numProcesses);
        this.callbackManager.increaseForNewProcesses(numProcesses);
//        this.pingManager.increaseForNewProcesses(numProcesses);
//        System.out.println("[ActivityExecutionContext] Now setup for " + this.numProcesses + " processes.");
    }

    public void scheduleTask(Runnable command, long initial, long period, TimeUnit unit) {
        this.executor.scheduleAtFixedRate(() -> {
            this.addActivity("", command);
        }, initial, period, unit);
    }

    public String queueStates() {
        return
            "Pending ~ Activities: " + this.activityManager.getQueueSize() +
            ", Callbacks: " + this.callbackManager.getQueueSize() +
            "  --  Total ~ Activities: " + this.activityManager.getTotalActivities() +
            ", Callbacks: " + this.callbackManager.getTotalActivities();
    }
}
