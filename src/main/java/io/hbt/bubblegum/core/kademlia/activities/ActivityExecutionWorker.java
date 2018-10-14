package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.auxiliary.ConcurrentBlockingQueue;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionManager.WorkItem;


public class ActivityExecutionWorker {

    private final ActivityExecutionManager manager;
    private final ConcurrentBlockingQueue<WorkItem> queue;
    private final Thread executionContext;
    private final int id;
    private boolean alive = true;

    public ActivityExecutionWorker(ActivityExecutionManager manager, int id, ConcurrentBlockingQueue<WorkItem> queue) {
        this.manager = manager;
        this.id = id;
        this.queue = queue;
        this.executionContext = new Thread(() -> this.start());
        this.executionContext.setDaemon(true);
        this.executionContext.start();
    }

    private void start() {
        WorkItem item;
        while(this.alive) {
            try {
                item = this.queue.get();
                if(item.getOperation() != null) item.getOperation().run();
                this.manager.callback(item.getOwner());

            } catch (InterruptedException e) {
                this.alive = false;
                this.print("ActivityExecutionWorker interrupted");
            }
        }
    }

    public void kill() {
        this.alive = false;
        this.executionContext.interrupt();
    }

    private void print(String msg) {
        System.out.println("[ActivityExecutionWorker #"+ this.id +"] " + msg);
    }
}
