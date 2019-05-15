package io.hbt.bubblegum.core.auxiliary;

import io.hbt.bubblegum.core.Configuration;

import java.util.LinkedList;
import java.util.Queue;


/**
 * Global buffer reuse manager.
 */
public class BufferPool {
    private static int available = 0;
    private static final Queue<byte[]> availableQueue = new LinkedList<>();

    /**
     * Re-purpose or create a new byte array for packet handling.
     * @return The byte array to be used.
     */
    public synchronized static byte[] getOrCreateBuffer() {
        if(BufferPool.available > 0) {
            BufferPool.available--;
            return BufferPool.availableQueue.poll();
        }
        else {
            byte[] newBuffer = new byte[Configuration.DATAGRAM_BUFFER_SIZE];
            return newBuffer;
        }
    }

    /**
     * Release a byte array for re-use.
     * @param arr The array to release.
     */
    public synchronized static void release(byte[] arr) {
        BufferPool.available++;
        BufferPool.availableQueue.add(arr);
    }

} // end BufferPool class
