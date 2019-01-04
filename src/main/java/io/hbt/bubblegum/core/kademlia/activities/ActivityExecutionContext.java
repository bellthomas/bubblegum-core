package io.hbt.bubblegum.core.kademlia.activities;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class ActivityExecutionContext {

    private final ScheduledThreadPoolExecutor executor;
    private final AtomicInteger currentActiveFibers = new AtomicInteger(0);
    private final static int MAX_ACTIVE_FIBERS = 500;
    private int failedActivities = 0;

    public ActivityExecutionContext() {
        executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);
        this.scheduleTask(() -> {
            System.out.println("Active Fibers: " + this.currentActiveFibers.get());
        }, 2, 2, TimeUnit.SECONDS);
    }

    @Suspendable
    private void createFiber(SuspendableRunnable r) {
        Fiber f = new Fiber<>(() -> {
            this.currentActiveFibers.incrementAndGet();
            r.run();
            this.currentActiveFibers.decrementAndGet();
        });
        f.start();
    }

    @Suspendable
    public void addActivity(String owner, SuspendableRunnable r) {
        if(this.currentActiveFibers.get() < MAX_ACTIVE_FIBERS) this.createFiber(r);
        else {
            System.err.println("Too many Fibers, activity dropped");
            this.failedActivities++;
        }
    }

    @Suspendable
    public void addPingActivity(String owner, SuspendableRunnable r) {
        this.addActivity(owner, r);
    }

    @Suspendable
    public void addCallbackActivity(String owner, SuspendableRunnable r) {
        this.addActivity(owner, r);
    }

    @Suspendable
    public void scheduleTask(SuspendableRunnable command, long initial, long period, TimeUnit unit) {
        this.executor.scheduleAtFixedRate(() -> {
            this.addActivity("", command);
        }, initial, period, unit);
    }
}

