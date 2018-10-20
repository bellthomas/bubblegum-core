package io.hbt.bubblegum.core.kademlia.router;

import com.sun.source.tree.AssertTree;
import io.hbt.bubblegum.core.kademlia.NodeID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class RouterBucketTest {

    static InetAddress localAddress;

    @BeforeAll
    static void setup() {
        try { localAddress = InetAddress.getLocalHost(); }
        catch (UnknownHostException e) {
            e.printStackTrace();
            assertTrue(false);
            return;
        }
    }

    @Test
    void add() {
        RouterBucket bucket = new RouterBucket(5);
        assertEquals(0, bucket.activeBucket.size());
        NodeID id = new NodeID();

        RouterNode n = new RouterNode(id, localAddress, 55555);
        bucket.add(n);

        long firstResponse = n.getLatestResponse();
        assertTrue(firstResponse > 0);
        assertEquals(1, bucket.activeBucket.size());

        try { Thread.sleep(100); }
        catch (InterruptedException e) { e.printStackTrace(); assertTrue(false); }

        // Check duplicates aren't added
        bucket.add(n);
        assertEquals(1, bucket.activeBucket.size());

        long secondResponse = n.getLatestResponse();
        assertTrue(secondResponse > firstResponse);

        // Check that duplicate ids are detected
        RouterNode n2 = new RouterNode(id, localAddress, 55555);
        bucket.add(n2);
        assertEquals(1, bucket.activeBucket.size());
    }

    @Test
    void addWithReplacing() {
        RouterBucket bucket = new RouterBucket(5);
        RouterNode n = new RouterNode(new NodeID(), localAddress, 55555);
        bucket.add(n);

        for(int i = 1; i < 8; i++) bucket.add(new RouterNode(new NodeID(), localAddress, 55555+i));

        assertEquals(8, bucket.activeBucket.size());
        assertEquals(0, bucket.replacements.size());

        RouterNode n2 = new RouterNode(new NodeID(), localAddress, 56000);
        bucket.add(n2);
        assertEquals(8, bucket.activeBucket.size());
        assertEquals(1, bucket.replacements.size());
        assertTrue(bucket.replacements.contains(n2));

        n.hasFailedToRespond();
        RouterNode n3 = new RouterNode(new NodeID(), localAddress, 56001);
        bucket.add(n3);

        assertEquals(8, bucket.activeNodes);
        assertEquals(2, bucket.replacementNodes);
        assertTrue(bucket.replacements.contains(n));
        assertTrue(bucket.activeBucket.contains(n3));

        System.out.println(bucket);
    }

    @Test
    void removeFromActiveTable() {
        RouterBucket bucket = new RouterBucket(5);
        RouterNode n = new RouterNode(new NodeID(), localAddress, 55555);
        bucket.add(n);

        assertTrue(bucket.activeBucket.contains(n));
        bucket.removeFromActiveTable(n);
        assertFalse(bucket.activeBucket.contains(n));

        RouterNode n2 =bucket.removeFromActiveTable(new RouterNode(new NodeID(), localAddress, 57555));
        assertNull(n2);
    }

    @Test
    void addToReplacements() {
        RouterBucket bucket = new RouterBucket(5);
        RouterNode[] nodes = new RouterNode[8];
        for(int i = 0; i < 8; i++) {
            nodes[i] = new RouterNode(new NodeID(), localAddress, 57555+i);
            nodes[i].hasResponded();
            bucket.addToReplacements(nodes[i]);
        }

        try { Thread.sleep(100); }
        catch (InterruptedException e) { e.printStackTrace(); assertTrue(false); }

        for(int i = 0; i < 8; i++) {
            long first = nodes[i].getLatestResponse();
            bucket.addToReplacements(nodes[i]);
            assertTrue(first < nodes[i].getLatestResponse());
        }

        bucket.addToReplacements(new RouterNode(new NodeID(), localAddress, 58555));

        assertEquals(8, bucket.replacementNodes);
    }

    @Test
    void getNodes() {
        RouterBucket bucket = new RouterBucket(5);

        assertNotNull(bucket.getNodes());
        assertEquals(0, bucket.getNodes().size());

        RouterNode n = new RouterNode(new NodeID(), localAddress, 55555);
        bucket.add(n);

        assertEquals(1, bucket.getNodes().size());
        assertTrue(bucket.getNodes().contains(n));
    }

    @Test
    void getRouterNodeWithID() {
        RouterBucket bucket = new RouterBucket(5);
        RouterNode[] ids = new RouterNode[16];
        for(int i = 0; i < 16; i++) {
            ids[i] = new RouterNode(new NodeID(), localAddress, 55555+i);
            bucket.add(ids[i]);
        }

        assertEquals(8, bucket.activeNodes);
        assertEquals(8, bucket.replacementNodes);

        for(int i = 0; i < 16; i++) {
            assertEquals(ids[i], bucket.getRouterNodeWithID(ids[i].getNode()));
        }

        assertNull(bucket.getRouterNodeWithID(new NodeID()));
    }

    @Test
    void getBucketSize() {
        RouterBucket bucket = new RouterBucket(5);
        for(int i = 0; i < 8; i++) {
            bucket.add(new RouterNode(new NodeID(), localAddress, 55555+i));
            assertEquals(i+1, bucket.getBucketSize());
        }

        for(int i = 0; i < 8; i++) {
            bucket.add(new RouterNode(new NodeID(), localAddress, 56555+i));
            assertEquals(8, bucket.getBucketSize());
        }
    }
}