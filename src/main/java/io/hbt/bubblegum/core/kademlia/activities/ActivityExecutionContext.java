package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.Configuration;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ActivityExecutionContext {

    private final ActivityExecutionManager activityManager;
    private final ActivityExecutionManager compoundManager;
    private final ActivityExecutionManager callbackManager;

    private final ScheduledThreadPoolExecutor executor;
    private int numProcesses = 0;

    public ActivityExecutionContext(int numProcesses) {
        this.numProcesses = (numProcesses < 0) ? 0 : numProcesses;
        if(numProcesses < 1) numProcesses = 1;

        double parallelismTotal =
            Configuration.EXECUTION_CONTEXT_GENERAL_PARALLELISM +
            Configuration.EXECUTION_CONTEXT_COMPOUND_PARALLELISM +
            Configuration.EXECUTION_CONTEXT_CALLBACK_PARALLELISM;

        executor = new ScheduledThreadPoolExecutor(2);
        executor.setRemoveOnCancelPolicy(true);

        this.activityManager = new ActivityExecutionManager(
            numProcesses, Configuration.EXECUTION_CONTEXT_GENERAL_PARALLELISM,
            (int)((Configuration.EXECUTION_CONTEXT_GENERAL_PARALLELISM / parallelismTotal) * Configuration.EXECUTION_CONTEXT_MAX_THREADS)
        );

        this.compoundManager = new ActivityExecutionManager(
            numProcesses, Configuration.EXECUTION_CONTEXT_COMPOUND_PARALLELISM,
            (int)((Configuration.EXECUTION_CONTEXT_COMPOUND_PARALLELISM / parallelismTotal) * Configuration.EXECUTION_CONTEXT_MAX_THREADS)
        );

        this.callbackManager = new ActivityExecutionManager(
            numProcesses, Configuration.EXECUTION_CONTEXT_CALLBACK_PARALLELISM,
            (int)((Configuration.EXECUTION_CONTEXT_CALLBACK_PARALLELISM / parallelismTotal) * Configuration.EXECUTION_CONTEXT_MAX_THREADS)
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
        this.executor.scheduleAtFixedRate(() -> this.addCompoundActivity(owner, command), initial, period, unit);
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
//        System.out.print(" [Load: " + String.format("%.2f", (systemLoad * 100)) + "%]");

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
