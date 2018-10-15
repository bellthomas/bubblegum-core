package io.hbt.bubblegum.core.kademlia.activities;

public class ActivityExecutionContext {

    private final ActivityExecutionManager activityManager;
    private final ActivityExecutionManager callbackManager;
    private final ActivityExecutionManager pingManager;

//    protected final static int MAX_THREADS_PER_MANAGER = 600;

    public ActivityExecutionContext(int numProcesses) {
        this.activityManager = new ActivityExecutionManager(numProcesses, 5);
        this.callbackManager = new ActivityExecutionManager(numProcesses, 10);
        this.pingManager = new ActivityExecutionManager(numProcesses, 5);
    }

    public void addActivity(String owner, Runnable r) {
        this.activityManager.addActivity(owner, r);
    }

    public void addPingActivity(String owner, Runnable r) {
        this.pingManager.addActivity(owner, r);
    }

    public void addCallbackActivity(String owner, Runnable r) {
        this.callbackManager.addActivity(owner, r);
    }

    public void addDelayedActivity(String owner, Runnable r, long milliseconds) {
        this.activityManager.addDelayedActivity(owner, r, milliseconds);
    }
}
