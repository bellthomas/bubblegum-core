package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.simulator.Metrics;

public abstract class SystemActivity implements Runnable {

    protected final static int TIMEOUT = 50; // 5 seconds

    protected final BubblegumNode localNode;
    protected boolean complete, success;

    private long init, start;

    public SystemActivity(BubblegumNode self) {
        this.localNode = self;
        this.complete = false;
        this.init = System.nanoTime();
    }

    @Override
    public void run() {
        this.start = System.nanoTime();
    }

    protected void print(String msg) {
        this.localNode.log(msg);
    }

    protected void timeoutOnComplete() {
        int i = 0;
        long end = System.currentTimeMillis() + 5000;
        while(System.currentTimeMillis() < end && !this.complete) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
        this.complete = true;
        this.success = false;
        this.recordMetrics();
    }

    protected void onFail(String message) {
        this.print(message);
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
