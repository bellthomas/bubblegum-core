package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.ConcurrentBlockingQueue;
import io.hbt.bubblegum.core.auxiliary.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Task-class worker pool management instance.
 */
public class ActivityExecutionManager {

    /**
     * Internal work item record,
     */
    protected class WorkItem {
        private String owner;
        private Runnable operation;
        protected WorkItem(String owner, Runnable op) {
            this.owner = owner;
            this.operation = op;
        }

        public Runnable getOperation() {
            return this.operation;
        }
        public String getOwner() {
            return this.owner;
        }
    }

    private int maximumNumberOfThreads;
    private int workerPoolSize;
    private List<ActivityExecutionWorker> workers;
    private ConcurrentBlockingQueue<WorkItem> queue;

    private int parallelism;
    private HashMap<String, Integer> parallelismMatrix;
    private HashMap<String, ConcurrentLinkedQueue<WorkItem>> backlog;

    private enum WorkerState { ACTIVE, IDLE }
    private HashMap<Integer, Pair<WorkerState, Long>> workerStates = new HashMap<>();
    private long active = 0L;
    private long idle = 0L;

    /**
     * Constructor.
     * @param numProcesses The number of processes to initialise the manager with.
     * @param parallelism The per-process thead parallelism factor.
     * @param maximum The maximum number of threads allowed.
     */
    public ActivityExecutionManager(int numProcesses, int parallelism, int maximum) {
        this.maximumNumberOfThreads = maximum;
        this.workerPoolSize = Math.min(maximumNumberOfThreads, numProcesses * parallelism);
        this.workers = new ArrayList<>(this.workerPoolSize);
        this.parallelism = parallelism;
        this.parallelismMatrix = new HashMap<>();
        this.backlog = new HashMap<>();
        this.queue = new ConcurrentBlockingQueue<>();
        this.initialiseWorkers();
    }

    /**
     * Initialise ActivityExecutionWorker instances.
     */
    private void initialiseWorkers() {
        for(int i = 0; i < this.workerPoolSize; i++) {
            this.workerStateChanged(i, WorkerState.IDLE);
            this.workers.add(new ActivityExecutionWorker(this, i, this.queue));
        }
    }

    /**
     * Create new workers, if permitted, for a new process.
     */
    public synchronized void increaseForNewProcess() {
        int numToCreate = (this.workerPoolSize + this.parallelism > this.maximumNumberOfThreads) ?
            this.maximumNumberOfThreads - this.workerPoolSize : this.parallelism;

        for(int i = 0; i < numToCreate; i++) {
            this.workerStateChanged(this.workerPoolSize + i, WorkerState.IDLE);
            this.workers.add(new ActivityExecutionWorker(this, this.workerPoolSize + i, this.queue));
        }
        this.workerPoolSize += numToCreate;
    }

    /**
     * Create new workers, if permitted, for new processes.
     * @param number the number of new processes.
     */
    public synchronized void increaseForNewProcesses(int number) {
        for(int i = 0; i < number; i++) this.increaseForNewProcess();
    }

    /**
     * Accept a new activity to asynchronously execute.
     * @param owner The owner's identifier.
     * @param r The runnable task.
     */
    public void addActivity(String owner, Runnable r) {
        synchronized (this.parallelismMatrix) {
            if (!this.parallelismMatrix.containsKey(owner)) this.parallelismMatrix.put(owner, 0);
        }
        synchronized (this.parallelismMatrix.get(owner)) {
            int current = this.parallelismMatrix.get(owner);
            if(current >= this.parallelism) {
                if(current < Configuration.EXECUTION_CONTEXT_MAX_PENDING_QUEUE) {
                    if (!this.backlog.containsKey(owner)) this.backlog.put(owner, new ConcurrentLinkedQueue<>());
                    this.backlog.get(owner).add(new WorkItem(owner, r));
                }
                else {
                    // Reject
                }
            }
            else {
                this.parallelismMatrix.put(owner, current + 1);
                this.queue.put(new WorkItem(owner, r));
            }
        }
    }

    /**
     * Register that a worker has transitioned to the active state.
     * @param worker the worker ID.
     * @param id
     */
    protected void onStart(int worker, String id) {
        this.workerStateChanged(worker, WorkerState.ACTIVE);
    }

    /**
     * Register that a worker has transitioned to the idle state and, if necessary, bring tasks out of the backlog.
     * @param worker The worker identifier.
     * @param owner The task's owner's identifier.
     * @param id
     */
    protected void callback(int worker, String owner, String id) {
        this.workerStateChanged(worker, WorkerState.IDLE);

        if(this.parallelismMatrix.containsKey(owner)) {
            synchronized (this.parallelismMatrix.get(owner)) {
                if(this.backlog.containsKey(owner) && !this.backlog.get(owner).isEmpty()) {
                    this.queue.put(this.backlog.get(owner).poll());
                }
                else {
                    this.parallelismMatrix.put(owner, Math.max(0, this.parallelismMatrix.get(owner) - 1));
                }
            }
        }
    }

    /**
     * Register a transition in a worker's state.
     * @param worker The worker's ID.
     * @param state The worker's new state.
     */
    private void workerStateChanged(int worker, WorkerState state) {
        if(!this.workerStates.containsKey(worker)) {
            this.workerStates.put(worker, new Pair<>(state, System.nanoTime()));
            return;
        }
        synchronized (this.workerStates) { /* To avoid race conditions */}
        synchronized (this.workerStates.get(worker)) {
            if (!this.workerStates.get(worker).getFirst().equals(state)) {
                long stateDuration = System.nanoTime() - this.workerStates.get(worker).getSecond();
                if (state.equals(WorkerState.ACTIVE)) this.active += stateDuration;
                else this.idle += stateDuration;
                this.workerStates.get(worker).setFirst(state);
                this.workerStates.get(worker).setSecond(System.nanoTime());
            }
        }
    }

    /**
     * Write out the task class's execution metrics.
     * @return The task class's recently CPU load.
     */
    protected float flushMetrics() {
        long currentlyActive = 0L;
        long currentlyIdle = 0L;
        long resultActive;
        long resultIdle;

        synchronized (this.workerStates) {
            long startTime = System.nanoTime();
            resultActive = this.active;
            resultIdle = this.idle;
            for(Map.Entry<Integer, Pair<WorkerState, Long>> entry : this.workerStates.entrySet()) {
                synchronized (entry.getValue()) {
                    if (entry.getValue().getFirst().equals(WorkerState.ACTIVE)) {
                        currentlyActive += (startTime - entry.getValue().getSecond());
                        entry.getValue().setSecond(startTime);
                    } else {
                        currentlyIdle += (startTime - entry.getValue().getSecond());
                        entry.getValue().setSecond(startTime);
                    }
                }
            }

            resultIdle += (currentlyIdle);
            resultActive += (currentlyActive);
            if(resultActive < 0) resultActive = 0;
            if(resultIdle < 0) resultIdle = 0;
            this.active = 0L;
            this.idle = 0L;
        }

        if(resultActive == 0 && resultIdle == 0) return 0;
        else return (float)resultActive / (resultActive + resultIdle);
    }

    /**
     * Retrieve the number of pending task class activities.
     * @return The count.
     */
    public int getQueueSize() {
        return this.queue.getItemCount();
    }

    /**
     * Retrieve the total throughput of the task class.
     * @return the total throughput.
     */
    public long getTotalActivities() {
        return this.queue.getTotal();
    }

} // end ActivityExecutionManager class