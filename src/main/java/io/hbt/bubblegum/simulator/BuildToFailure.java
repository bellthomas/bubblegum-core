package io.hbt.bubblegum.simulator;

import io.hbt.bubblegum.core.Bubblegum;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class BuildToFailure {

    private Bubblegum bubblegum;
    private int buffer = 1000;

    private boolean stillRunning = true;

    private int backgroundSoftFails = 0;
    private final AtomicInteger progress = new AtomicInteger(0);
    private Queue<Runnable> bootstrapActions = new LinkedList<>();

    public BuildToFailure() {
        this.bubblegum = new Bubblegum(false);

        long start = System.currentTimeMillis();
        System.out.println(" Running build to failure test...");
        BubblegumNode genesisNode = this.bubblegum.createNode();
        for(int i = 0; i < this.buffer; i++) {
            this.bootstrapActions.add(() -> {
                try {
                    this.runBootstrap(genesisNode);
                } catch (Exception e) {
                    this.stillRunning = false;
                }
            });
        }

        int threadPoolSize = 30;
        Thread[] executors = new Thread[threadPoolSize];
        for(int i = 0; i < threadPoolSize; i++) {
            executors[i] = new Thread(() -> {
                Runnable task;
                while(this.stillRunning) {
                    synchronized (bootstrapActions) {
                        task = bootstrapActions.poll();
                    }

                    if(task != null) task.run();
                    if(progress.incrementAndGet() % 100 == 0) System.gc();
                }
            });
            executors[i].setDaemon(true);
            executors[i].start();
        }

        while(this.stillRunning) {
            System.out.print(" ["+(System.currentTimeMillis() - start)+"ms] " + progress.get() + " (" + this.backgroundSoftFails + ") -- "+this.bubblegum.getExecutionContext().queueStates()+"\r");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for(Thread t : executors) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println(" ["+(System.currentTimeMillis() - start)+"ms] Final Count Before Failure: " + progress.get() + "\r");
        System.exit(0);
    }

    private boolean runBootstrap(BubblegumNode oldNode, BubblegumNode newNode, int attempt) {
        boolean success = newNode.bootstrap(
            oldNode.getServer().getLocal(),
            oldNode.getServer().getPort(),
            oldNode.getRecipientID()
        );

        if(!success || newNode.getRoutingTable().getSize() < 2) {
            this.backgroundSoftFails++;
            if(attempt >= 5) return false;
            else bootstrapActions.add(() -> this.runBootstrap(oldNode, newNode, attempt + 1));
        }
        else {
            this.progress.incrementAndGet();
            return true;
        }

        return false;
    }

    private void runBootstrap(BubblegumNode oldNode) {
        BubblegumNode newNode = this.bubblegum.createNode();
        if(this.runBootstrap(oldNode, newNode, 1)) this.bootstrapActions.add(() -> this.runBootstrap(oldNode));
        else this.stillRunning = false;
    }

    public static void main(String[] args) {
        Simulator.currentlySimulating = true;
        NetworkingHelper.setLookupExternalIP(false);
        new BuildToFailure();
    }
}
