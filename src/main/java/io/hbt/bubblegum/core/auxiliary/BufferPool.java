package io.hbt.bubblegum.core.auxiliary;

import io.hbt.bubblegum.core.Configuration;

import java.util.LinkedList;
import java.util.Queue;

public class BufferPool {
    private static int totalCreated = 0;
    private static int available = 0;
    private static final Queue<byte[]> availableQueue = new LinkedList<>();

    public synchronized static byte[] getOrCreateBuffer() {
        if(BufferPool.available > 0) {
            BufferPool.available--;
            return BufferPool.availableQueue.poll();
        }
        else {
            byte[] newBuffer = new byte[Configuration.DATAGRAM_BUFFER_SIZE];
            BufferPool.totalCreated++;
//            System.out.println("[BufferPool] " + totalCreated + " buffers now created");
            return newBuffer;
        }
    }

    public synchronized static void release(byte[] arr) {
        //Arrays.fill(arr, (byte)0);
        BufferPool.available++;
        BufferPool.availableQueue.add(arr);
    }
}
