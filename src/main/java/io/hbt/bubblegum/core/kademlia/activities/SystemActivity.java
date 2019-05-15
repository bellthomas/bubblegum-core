package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.simulator.Metrics;

/**
 * The generic template for RPC/compound activities in bubblegum-core.
 */
public abstract class SystemActivity implements Runnable {

    protected final BubblegumNode localNode;
    protected boolean complete, success, aborted;
    private long init, start;

    /**
     * Constructor.
     * @param self The BubblegumNode that created the activity.
     */
    public SystemActivity(BubblegumNode self) {
        this.localNode = self;
        this.complete = false;
        this.aborted = false;
        this.init = System.nanoTime();
    }

    /**
     * Start the activity, managing the timeout timers and abort checks.
     */
    @Override
    public void run() {
        this.start = System.nanoTime();
        if(this.start > (Configuration.ACTIVITY_MAX_DELAY * 1_000_000 + this.init)) {
            this.aborted = true;
        }
    }

    /**
     * Log a message.
     * @param msg The message.
     */
    protected void print(String msg) {
        this.localNode.log(msg);
    }

    /**
     * Wait for the activity to complete or timeout.
     */
    protected void timeoutOnComplete() {
        int i = 0;
        long end = System.currentTimeMillis() + Configuration.ACTIVITY_TIMEOUT;
        while(System.currentTimeMillis() < end && !this.complete) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(!this.complete) this.onFail();
    }

    /**
     * Declare that the activity has successfully completed.
     */
    protected void onSuccess() {
        this.complete = true;
        this.success = true;
        this.recordMetrics();
    }

    /**
     * Declare that the activity has successfully completed.
     * @param message A log message.
     */
    protected void onSuccess(String message) {
        this.print(message);
        this.onSuccess();
    }

    /**
     * Declare that the activity has unsuccessfully completed.
     */
    protected void onFail() {
        this.complete = true;
        this.success = false;
        this.recordMetrics();
    }

    /**
     * Declare that the activity has unsuccessfully completed.
     * @param message A log message.
     */
    protected void onFail(String message) {
        this.print(message);
        this.onFail();
    }

    /**
     * Record activity metrics if required.
     */
    private void recordMetrics() {
        if(Metrics.isRecording())
            Metrics.activitySubmission(this, this.init, this.start, System.nanoTime(), this.success);
    }

    /**
     * Check if the activity has completed.
     * @return Completion status.
     */
    public boolean getComplete() {
        return this.complete;
    }

    /**
     * Check if the activity has succeeded.
     * @return Success status.
     */
    public boolean getSuccess() {
        return this.success;
    }

} // end SystemActivity class
