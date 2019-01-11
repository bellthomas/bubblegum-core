package io.hbt.bubblegum.core.kademlia.activities;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ActivityExecutionContext {

    private final ActivityExecutionManager activityManager;
    private final ActivityExecutionManager compoundManager;
    private final ActivityExecutionManager callbackManager;

    protected final static int MAX_THREADS_IN_CONTEXT = 200;

    protected final static int GENERAL_ACTIVITIES_PARALLELISM = 8;//.4
    protected final static int COMPOUND_ACTIVITIES_PARALLELISM = 5;//.25
    protected final static int CALLBACK_ACTIVITIES_PARALLELISM = 7;//

    private int numProcesses = 0;

    private final ScheduledThreadPoolExecutor executor;

    public ActivityExecutionContext(int numProcesses) {
        this.numProcesses = (numProcesses < 0) ? 0 : numProcesses;
        if(numProcesses < 1) numProcesses = 1;

        double parallelismTotal = GENERAL_ACTIVITIES_PARALLELISM + CALLBACK_ACTIVITIES_PARALLELISM;

        executor = new ScheduledThreadPoolExecutor(2);
        executor.setRemoveOnCancelPolicy(true);

        this.activityManager = new ActivityExecutionManager(
            numProcesses, GENERAL_ACTIVITIES_PARALLELISM,
            (int)((GENERAL_ACTIVITIES_PARALLELISM / parallelismTotal) * MAX_THREADS_IN_CONTEXT)
        );

        this.compoundManager = new ActivityExecutionManager(
            numProcesses, COMPOUND_ACTIVITIES_PARALLELISM,
            (int)((COMPOUND_ACTIVITIES_PARALLELISM / parallelismTotal) * MAX_THREADS_IN_CONTEXT)
        );

        this.callbackManager = new ActivityExecutionManager(
            numProcesses, CALLBACK_ACTIVITIES_PARALLELISM,
            (int)((CALLBACK_ACTIVITIES_PARALLELISM / parallelismTotal) * MAX_THREADS_IN_CONTEXT)
        );
    }

    public void addActivity(String owner, Runnable r) {
        this.activityManager.addActivity(owner, r);
    }

    public void addCompoundActivity(String owner, Runnable r) {
        this.compoundManager.addActivity(owner, r);
    }

    public void addCallbackActivity(String owner, Runnable r) {
        this.callbackManager.addActivity(owner, r);
    }

    public void newProcessInContext() {
        this.numProcesses++;
        if(this.numProcesses > 1) {
            this.activityManager.increaseForNewProcess();
            this.callbackManager.increaseForNewProcess();
            this.compoundManager.increaseForNewProcess();
        }
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
        this.compoundManager.increaseForNewProcesses(numProcesses);
    }

    public void scheduleTask(String owner, Runnable command, long initial, long period, TimeUnit unit) {
        this.executor.scheduleAtFixedRate(() -> {
            this.addCompoundActivity(owner, command);
        }, initial, period, unit);
    }

    public String queueStates() {
        return
            "Pending ~ Activities: " + this.activityManager.getQueueSize() +
            ", Compound: " + this.compoundManager.getQueueSize() +
            ", Callbacks: " + this.callbackManager.getQueueSize(); //+
//            "  --  Total ~ Activities: " + this.activityManager.getTotalActivities() + " (" + this.activityManager.getAverageExecutionTime() + "ms)" +
//            ", Compound: " + this.compoundManager.getTotalActivities() + " (" + this.compoundManager.getAverageExecutionTime() + "ms)" +
//            ", Callback: " + this.callbackManager.getTotalActivities() + " (" + this.callbackManager.getAverageExecutionTime() + "ms)";
    }


    public String queueLogHeader() {
        return "pendingActivities,pendingCompounds,pendingCallbacks,totalActivities,totalCompounds,totalCallbacks,avgActivityLoad,avgCompoundLoad,avgCallbackLoad,avgLoad";
    }

    public String queueLogInfo() {
        float activityLoad = this.activityManager.flushMetrics();
        float compoundLoad = this.compoundManager.flushMetrics();
        float callbackLoad = this.callbackManager.flushMetrics();
        float systemLoad = Math.max(activityLoad, Math.max(compoundLoad, callbackLoad));
        System.out.print(" [Load: " + String.format("%.2f", (systemLoad * 100)) + "%]");

        StringBuilder sb = new StringBuilder();
        sb.append(this.activityManager.getQueueSize() + ",");
        sb.append(this.compoundManager.getQueueSize() + ",");
        sb.append(this.callbackManager.getQueueSize() + ",");
        sb.append(this.activityManager.getTotalActivities() + ",");
        sb.append(this.compoundManager.getTotalActivities() + ",");
        sb.append(this.callbackManager.getTotalActivities() + ",");
        sb.append(activityLoad + ",");
        sb.append(compoundLoad + ",");
        sb.append(callbackLoad + ",");
        sb.append(systemLoad);
        return sb.toString();
    }
}
