package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.simulator.Metrics;

public abstract class SystemActivity implements Runnable {

    protected final static int TIMEOUT = 10000; // ms
    protected final static int MAX_START_DELAY = 10000; // ms

    protected final BubblegumNode localNode;
    protected boolean complete, success, aborted;

    private long init, start;

    public SystemActivity(BubblegumNode self) {
        this.localNode = self;
        this.complete = false;
        this.aborted = false;
        this.init = System.nanoTime();
    }

    @Override
    public void run() {
        this.start = System.nanoTime();
        if(this.start > (MAX_START_DELAY * 1_000_000 + this.init)) {
            this.aborted = true;
        }
    }

    protected void print(String msg) {
        this.localNode.log(msg);
    }

    protected void timeoutOnComplete() {
        int i = 0;
        long end = System.currentTimeMillis() + TIMEOUT;
        while(System.currentTimeMillis() < end && !this.complete) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(!this.complete) this.onFail();
    }

    protected void onSuccess() {
        this.complete = true;
        this.success = true;
        this.recordMetrics();
    }

    protected void onSuccess(String message) {
        this.print(message);
        this.onSuccess();
    }

    protected void onFail() {
//        System.out.println("Failed: " + this.getClass().getSimpleName());
        this.complete = true;
        this.success = false;
        this.recordMetrics();
    }

    protected void onFail(String message) {
//        System.out.println(message);
        this.onFail();
    }

    private void recordMetrics() {
        if(Metrics.isRecording())
            Metrics.activitySubmission(this, this.init, this.start, System.nanoTime(), this.success);
    }

    public boolean getComplete() {
        return this.complete;
    }

    public boolean getSuccess() {
        return this.success;
    }

}
