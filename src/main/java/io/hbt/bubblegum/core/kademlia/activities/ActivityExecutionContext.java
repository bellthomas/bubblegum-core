package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.Configuration;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * The asynchronous execution context of a Bubblegum instance.
 */
public class ActivityExecutionContext {

    private final ActivityExecutionManager activityManager;
    private final ActivityExecutionManager compoundManager;
    private final ActivityExecutionManager callbackManager;

    private final ScheduledThreadPoolExecutor executor;
    private int numProcesses = 0;

    /**
     * Constructor.
     * @param numProcesses The number of nodes to initialise the managers for.
     */
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

    /**
     * New task for class "Activity".
     * @param owner The owner's identifier.
     * @param r The executable task.
     */
    public void addActivity(String owner, Runnable r) {
        this.activityManager.addActivity(owner, r);
    }

    /**
     * New task for class "Compound".
     * @param owner The owner's identifier.
     * @param r The executable task.
     */
    public void addCompoundActivity(String owner, Runnable r) {
        this.compoundManager.addActivity(owner, r);
    }

    /**
     * New task for class "Callback".
     * @param owner The owner's identifier.
     * @param r The executable task.
     */
    public void addCallbackActivity(String owner, Runnable r) {
        this.callbackManager.addActivity(owner, r);
    }

    /**
     * Increase allocations to execution managers.
     */
    public void newProcessInContext() {
        this.numProcesses++;
        if(this.numProcesses > 1) {
            this.activityManager.increaseForNewProcess();
            this.callbackManager.increaseForNewProcess();
            this.compoundManager.increaseForNewProcess();
        }
    }

    /**
     * Increase allocations to execution managers for multiple new processes.
     * @param numProcesses the number of new processes.
     */
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

    /**
     * Schedule a task for periodic execution.
     * @param owner The owner's identifier.
     * @param command The executable command.
     * @param initial The initial delay.
     * @param period The period between invocations.
     * @param unit The units of times given.
     */
    public void scheduleTask(String owner, Runnable command, long initial, long period, TimeUnit unit) {
        this.executor.scheduleAtFixedRate(() -> {
            this.addCompoundActivity(owner, command);
        }, initial, period, unit);
    }

    /**
     * Format the current states of the execution managers.
     * @return
     */
    public String queueStates() {
        return
            "Pending ~ Activities: " + this.activityManager.getQueueSize() +
            ", Compound: " + this.compoundManager.getQueueSize() +
            ", Callbacks: " + this.callbackManager.getQueueSize();
    }

    /**
     * CSV metrics output headers.
     * @return
     */
    public String queueLogHeader() {
        return "pendingActivities,pendingCompounds,pendingCallbacks,totalActivities,totalCompounds,totalCallbacks,avgActivityLoad,avgCompoundLoad,avgCallbackLoad,avgLoad";
    }

    /**
     * CSV metrics output values.
     * @return
     */
    public String queueLogInfo() {
        float activityLoad = this.activityManager.flushMetrics();
        float compoundLoad = this.compoundManager.flushMetrics();
        float callbackLoad = this.callbackManager.flushMetrics();
        float systemLoad = Math.max(activityLoad, Math.max(compoundLoad, callbackLoad));

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

} // end ActivityExecutionContext class
