package io.hbt.bubblegum.core.kademlia.activities;

public class ActivityExecutionContext {

    private final ActivityExecutionManager activityManager;
    private final ActivityExecutionManager callbackManager;
    private final ActivityExecutionManager pingManager;

    protected final static int MAX_THREADS_IN_CONTEXT = 2000;

    protected final static int GENERAL_ACTIVITY_PARALLELISM = 5;
    protected final static int CALLBACK_ACTIVITY_PARALLELISM = 10;
    protected final static int PING_ACTIVITY_PARALLELISM = 5;

    private int numProcesses = 0;

    public ActivityExecutionContext(int numProcesses) {
        this.numProcesses = (numProcesses < 0) ? 0 : numProcesses;
        if(numProcesses < 1) numProcesses = 1;

        double parallelismTotal = GENERAL_ACTIVITY_PARALLELISM + CALLBACK_ACTIVITY_PARALLELISM + PING_ACTIVITY_PARALLELISM;

        this.activityManager = new ActivityExecutionManager(
                numProcesses, GENERAL_ACTIVITY_PARALLELISM,
                (int)((GENERAL_ACTIVITY_PARALLELISM / parallelismTotal) * MAX_THREADS_IN_CONTEXT)
        );

        this.callbackManager = new ActivityExecutionManager(
                numProcesses, CALLBACK_ACTIVITY_PARALLELISM,
                (int)((CALLBACK_ACTIVITY_PARALLELISM / parallelismTotal) * MAX_THREADS_IN_CONTEXT)
        );

        this.pingManager = new ActivityExecutionManager(
                numProcesses,
                PING_ACTIVITY_PARALLELISM,
                (int)((PING_ACTIVITY_PARALLELISM / parallelismTotal) * MAX_THREADS_IN_CONTEXT)
        );
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

    public void newProcessInContext() {
        this.numProcesses++;
        if(this.numProcesses > 1) {
            this.activityManager.increaseForNewProcess();
            this.callbackManager.increaseForNewProcess();
            this.pingManager.increaseForNewProcess();
        }
        System.out.println("[ActivityExecutionContext] Now setup for " + this.numProcesses + " processes.");
    }

    public void newProcessesInContext(int numProcesses) {
        if(this.numProcesses == 0) numProcesses--;
        this.numProcesses += numProcesses;

        this.activityManager.increaseForNewProcesses(numProcesses);
        this.callbackManager.increaseForNewProcesses(numProcesses);
        this.pingManager.increaseForNewProcesses(numProcesses);
        System.out.println("[ActivityExecutionContext] Now setup for " + this.numProcesses + " processes.");
    }
}

/**


 minimum = numNetworks * parallelism
 maximum = 2000


 */