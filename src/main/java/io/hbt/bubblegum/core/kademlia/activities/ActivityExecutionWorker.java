package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.auxiliary.ConcurrentBlockingQueue;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionManager.WorkItem;


public class ActivityExecutionWorker {

    private final ActivityExecutionManager manager;
    private final ConcurrentBlockingQueue<WorkItem> queue;
    private Thread executionContext;
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
        long start, elapsed;
        while(this.alive) {
            try {
                item = this.queue.get();
                if(item != null) {
                    if (item.getOperation() != null) {
                        start = System.currentTimeMillis();
                        item.getOperation().run();
                        if(!item.getOwner().equals("system")) {
                            elapsed = System.currentTimeMillis() - start;
                            this.manager.declareExecutionTime(elapsed);
                        }
                    }
                    this.manager.callback(item.getOwner());
                }
                else {
                    break;
                }

            } catch (InterruptedException e) {
                this.alive = false;
                this.print("ActivityExecutionWorker interrupted");
            } catch (Exception e) {
                this.executionContext = new Thread(() -> this.start());
                this.executionContext.setDaemon(true);
                this.executionContext.start();
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