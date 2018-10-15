package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;

public abstract class SystemActivity implements Runnable {

    protected final static int TIMEOUT = 50; // 5 seconds

    protected final BubblegumNode localNode;
    protected boolean complete;

    public SystemActivity(BubblegumNode self) {
        this.localNode = self;
        this.complete = false;
    }


    protected void print(String msg) {
        this.localNode.log(msg);
    }

    public boolean getComplete() {
        return this.complete;
    }

    protected void timeoutOnComplete() {
        int i = 0;
        while(i < NetworkActivity.TIMEOUT && !this.complete) {
            try { Thread.sleep(100); }
            catch (InterruptedException e) { e.printStackTrace(); }
            i++;
        }
    }
}
