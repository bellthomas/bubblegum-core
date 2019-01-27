package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.ConcurrentBlockingQueue;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionManager.WorkItem;

import java.util.UUID;

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
        while(this.alive) {
            try {
                item = this.queue.get();
                if(item != null) {
                    String id = new UUID(Configuration.rand.nextLong(), Configuration.rand.nextLong()).toString();
                    this.manager.onStart(this.id, id);
                    if (item.getOperation() != null) {
                        item.getOperation().run();
                    }
                    this.manager.callback(this.id, item.getOwner(), id);
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