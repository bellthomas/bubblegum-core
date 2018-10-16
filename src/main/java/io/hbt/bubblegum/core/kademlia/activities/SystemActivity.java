package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;

public abstract class SystemActivity implements Runnable {

    protected final static int TIMEOUT = 50; // 5 seconds

    protected final BubblegumNode localNode;
    protected boolean complete, success;

    public SystemActivity(BubblegumNode self) {
        this.localNode = self;
        this.complete = false;
    }


    protected void print(String msg) {
        this.localNode.log(msg);
    }

    protected void timeoutOnComplete() {
        int i = 0;
        while(i < NetworkActivity.TIMEOUT && !this.complete) {
            try { Thread.sleep(100); }
            catch (InterruptedException e) { e.printStackTrace(); }
            i++;
        }
    }

    protected void onSuccess() {
        this.complete = true;
        this.success = true;
    }

    protected void onFail() {
        this.complete = true;
        this.success = false;
    }

    public boolean getComplete() {
        return this.complete;
    }

    public boolean getSuccess() {
        return this.success;
    }

}
