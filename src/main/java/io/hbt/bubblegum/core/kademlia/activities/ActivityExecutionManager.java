package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.auxiliary.ConcurrentBlockingQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

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

    public final static int MAX_THREADS = 1000;

    private int workerPoolSize;
    private final ConcurrentBlockingQueue<WorkItem> queue;
    private final List<ActivityExecutionWorker> workers;
    private final ScheduledExecutorService executor;

    private final int parallelism;
    private final HashMap<String, Integer> parallelismMatrix;
    private final HashMap<String, ConcurrentLinkedQueue<WorkItem>> backlog;

    public ActivityExecutionManager(int numProcesses, int parallelism) {
        this.workerPoolSize = Math.min(MAX_THREADS, numProcesses * parallelism);
        this.queue = new ConcurrentBlockingQueue<>();
        this.workers = new ArrayList<>(this.workerPoolSize);
        this.executor = new ScheduledThreadPoolExecutor(2);
        this.parallelism = parallelism;
        this.parallelismMatrix = new HashMap<>();
        this.backlog = new HashMap<>();
        this.initialiseWorkers();
    }

    private void initialiseWorkers() {
        for(int i = 0; i < this.workerPoolSize; i++) {
            this.workers.add(new ActivityExecutionWorker(this, i, this.queue));
        }
    }

    public synchronized void increaseForNewProcess() {
        for(int i = 0; i < this.parallelism; i++) {
            this.workers.add(new ActivityExecutionWorker(this, this.workerPoolSize + i, this.queue));
        }
        this.workerPoolSize += this.parallelism;
    }

    public synchronized void increaseForNewProcesses(int number) {
        for(int i = 0; i < number; i++) this.increaseForNewProcess();
    }

    // TODO synchronise per ID?
    public void addActivity(String owner, Runnable r) {
        if(!this.parallelismMatrix.containsKey(owner)) this.parallelismMatrix.put(owner, 0);
        synchronized (this.parallelismMatrix) {
            int current = this.parallelismMatrix.get(owner);
            if(current >= this.parallelism) {
                if(!this.backlog.containsKey(owner)) this.backlog.put(owner, new ConcurrentLinkedQueue<>());
                this.backlog.get(owner).add(new WorkItem(owner, r));
            }
            else {
                this.parallelismMatrix.put(owner, current + 1);
                this.queue.put(new WorkItem(owner, r));
            }
        }
    }

    public void addDelayedActivity(String owner, Runnable r, long milliseconds) {
        this.executor.schedule(() -> this.addActivity(owner, r), milliseconds, TimeUnit.MILLISECONDS);
    }

    protected void callback(String owner) {
        if(this.parallelismMatrix.containsKey(owner)) {
            synchronized (this.parallelismMatrix) {
                if(this.backlog.containsKey(owner) && !this.backlog.get(owner).isEmpty()) {
                    this.queue.put(this.backlog.get(owner).poll());
                }
                else {
                    this.parallelismMatrix.put(owner, Math.max(0, this.parallelismMatrix.get(owner) - 1));
                }
            }
        }
    }
}
