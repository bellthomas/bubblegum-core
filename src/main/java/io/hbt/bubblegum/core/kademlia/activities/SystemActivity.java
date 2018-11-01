package io.hbt.bubblegum.core.kademlia.activities;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;

public abstract class SystemActivity implements SuspendableRunnable {

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

    @Suspendable
    protected void timeoutOnComplete() {
        int i = 0;
        long end = System.currentTimeMillis() + 5000;
        while(System.currentTimeMillis() < end && !this.complete) {

            try {
                Strand.sleep(100);
            } catch (SuspendExecution suspendExecution) {
                suspendExecution.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
//        while(i < NetworkActivity.TIMEOUT && !this.complete) {
//            try { Strand.sleep(100); }
//            catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (SuspendExecution suspendExecution) {
//                suspendExecution.printStackTrace();
//            }
//            i++;
//        }
    }

    protected void onSuccess() {
        this.complete = true;
        this.success = true;
    }

    protected void onSuccess(String message) {
        this.print(message);
        this.onSuccess();
    }

    protected void onFail() {
        this.complete = true;
        this.success = false;
    }

    protected void onFail(String message) {
        this.print(message);
        this.onFail();
    }

    public boolean getComplete() {
        return this.complete;
    }

    public boolean getSuccess() {
        return this.success;
    }

}
