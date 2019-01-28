package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.ConcurrentBlockingQueue;
import io.hbt.bubblegum.core.auxiliary.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ActivityExecutionManager {

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
    private int numberOfProcesses;
    private int workerPoolSize;
    private List<ActivityExecutionWorker> workers;
    private ConcurrentBlockingQueue<WorkItem> queue;

    private int parallelism;
    private HashMap<String, Integer> parallelismMatrix;
    private HashMap<String, ConcurrentLinkedQueue<WorkItem>> backlog;

    public ActivityExecutionManager(int numProcesses, int parallelism, int maximum) {
        this.numberOfProcesses = numProcesses;
        this.maximumNumberOfThreads = maximum;
        this.workerPoolSize = Math.min(maximumNumberOfThreads, numProcesses * parallelism);
        this.workers = new ArrayList<>(this.workerPoolSize);
        this.parallelism = parallelism;
        this.parallelismMatrix = new HashMap<>();
        this.backlog = new HashMap<>();
        this.queue = new ConcurrentBlockingQueue<>();
        this.initialiseWorkers();
    }

    private void initialiseWorkers() {
        for(int i = 0; i < this.workerPoolSize; i++) {
            this.workerStateChanged(i, WorkerState.IDLE);
            this.workers.add(new ActivityExecutionWorker(this, i, this.queue));
        }
    }

    public synchronized void increaseForNewProcess() {
        this.numberOfProcesses++;

        int numToCreate = (this.workerPoolSize + this.parallelism > this.maximumNumberOfThreads) ?
            this.maximumNumberOfThreads - this.workerPoolSize : this.parallelism;

        for(int i = 0; i < numToCreate; i++) {
            this.workerStateChanged(this.workerPoolSize + i, WorkerState.IDLE);
            this.workers.add(new ActivityExecutionWorker(this, this.workerPoolSize + i, this.queue));
        }
        this.workerPoolSize += numToCreate;
    }

    public synchronized void increaseForNewProcesses(int number) {
        for(int i = 0; i < number; i++) this.increaseForNewProcess();
    }

    // TODO synchronise per ID?
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


    private enum WorkerState { ACTIVE, IDLE }

    protected void onStart(int worker, String id) {
        this.workerStateChanged(worker, WorkerState.ACTIVE);
//        synchronized (this.currentExecutingStartTimes) {
//            this.currentExecutingStartTimes.put(id, System.nanoTime());
//        }
    }

    protected void callback(int worker, String owner, String id) {
        this.workerStateChanged(worker, WorkerState.IDLE);

//        if (this.currentExecutingStartTimes.containsKey(id)) {
//            synchronized (this.currentExecutingStartTimes) {
//                this.currentExecutingStartTimes.remove(id);
//                this.completedExecution.addAndGet(System.nanoTime() - this.currentExecutingStartTimes.get(id));
//            }
//        }

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

    private HashMap<Integer, Pair<WorkerState, Long>> workerStates = new HashMap<>();
    private long active = 0L;
    private long idle = 0L;

    private void workerStateChanged(int worker, WorkerState state) {
        if(!this.workerStates.containsKey(worker)) {
            this.workerStates.put(worker, new Pair<>(state, System.nanoTime()));
            return;
        }
        synchronized (this.workerStates) { /* To avoid race conditions */}
        synchronized (this.workerStates.get(worker)) {
            if (!this.workerStates.get(worker).getFirst().equals(state)) {
                // state change
                long stateDuration = System.nanoTime() - this.workerStates.get(worker).getSecond();
                if (state.equals(WorkerState.ACTIVE)) this.active += stateDuration;
                else this.idle += stateDuration;
                this.workerStates.get(worker).setFirst(state);
                this.workerStates.get(worker).setSecond(System.nanoTime());
            }
        }
    }

    protected float flushMetrics() {
//        long currentlyExecuting = 0L;
//        long startTime = System.nanoTime();
//        long result = this.completedExecution.getAndSet(0L);
//        synchronized (this.currentExecutingStartTimes) {
//            for(Map.Entry<String, Long> entry : this.currentExecutingStartTimes.entrySet()) {
//                currentlyExecuting += startTime - entry.getValue();
//            }
//            result -= this.executionAlreadyDeclared;
//            result += currentlyExecuting;
//            this.executionAlreadyDeclared = currentlyExecuting;
//        }
//        return (double)result / 1_000_000;

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
            if(resultActive < 0) resultActive = 0; // TODO fix?
            if(resultIdle < 0) resultIdle = 0;
            this.active = 0L;
            this.idle = 0L;
        }

        if(resultActive == 0 && resultIdle == 0) return 0;
        else return (float)resultActive / (resultActive + resultIdle);
//        return 0;
    }

    public int getQueueSize() {
        return this.queue.getItemCount();
    }

    public long getTotalActivities() {
        return this.queue.getTotal();
    }

}