package io.hbt.bubblegum.core.kademlia.activities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActivityExecutionManagerTest {

    @Test
    void parallelism() {
        ActivityExecutionManager manager = new ActivityExecutionManager(3, 2);
        Runnable threeSeconds = () -> {
            try {
                Thread.sleep(1000);
                System.out.println("done");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        for(int i = 0; i < 10; i++) manager.addActivity("a", threeSeconds);

        System.out.println();

        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}