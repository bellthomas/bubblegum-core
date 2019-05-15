package io.hbt.bubblegum.core.auxiliary;

import java.util.LinkedList;
import java.util.Queue;


/**
 * An indefinitely-blocking concurrent access queue.
 * @param <T> The queue item type.
 */
public class ConcurrentBlockingQueue<T> implements Comparable<ConcurrentBlockingQueue<T>> {

    /**
     * Internal data store.
     * No need for thread safety here as the getter and putter provide this.
     */
    private Queue<T> items = new LinkedList<>();
    private int itemCountEstimate = 0;
    private long total = 0;

    /**
     * When set get() method will no longer accept any incoming threads, simply returning null.
     */
    private boolean flushQueueFlag = false;


    /**
     * Putter function.
     * Adds items to the data store and wakes up a consumer (if there is one waiting).
     * @param item Object to add to the data store.
     */
    public synchronized void put(T item) {
        this.items.add(item);
        this.total++;
        this.itemCountEstimate++;
        notify();
    }


    /**
     * Getter function.
     * Sleeps consumer threads if the are no items to consume, otherwise pops and returns the head of the list.
     *
     * @note The docs are unclear about whether the .size() method is O(1) for a LinkedList object. If this operation
     * is O(n) then it will probably be beneficial to remove the debug call.
     *
     * @return The item at the head of the list or null on erroneous interruption of a sleeping thread.
     * @throws InterruptedException
     */
    public synchronized T get() throws InterruptedException {
        while(this.items.size() == 0) {
            if(this.flushQueueFlag) return null;
            wait();
        }

        this.itemCountEstimate--;
        return this.items.poll();
    }


    /**
     * Wake up all waiting threads and don't allow any new threads to enter (returns null).
     */
    public synchronized void flushQueue() {
        this.flushQueueFlag = true;
        notifyAll();
    }


    /**
     * For Testing Purposes.
     * Copies the current state of the queue and makes it available for inspection.
     * @return The current state of the queue.
     */
    public synchronized Queue<T> getCurrentQueueState() {
        LinkedList<T> copy = new LinkedList<>();
        for(T item : this.items) copy.add(item);
        return copy;
    }




    public synchronized int getItemCount() {
        return this.itemCountEstimate;
    }

    public synchronized  long getTotal() {
        return this.total;
    }

    @Override
    public int compareTo(ConcurrentBlockingQueue<T> o) {
        return this.itemCountEstimate - o.itemCountEstimate;
    }

} // end ConcurrentBlockingQueue class