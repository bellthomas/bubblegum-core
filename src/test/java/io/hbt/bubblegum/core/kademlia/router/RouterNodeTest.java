package io.hbt.bubblegum.core.kademlia.router;

import io.hbt.bubblegum.core.kademlia.NodeID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class RouterNodeTest {

    static InetAddress local;

    @BeforeAll
    static void setup() {
        try {
            local = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            assertTrue(false);
            e.printStackTrace();
            return;
        }
    }

    @Test
    void hasResponded() {
        RouterNode n = new RouterNode(new NodeID(), local, 55555);
        assertEquals(0, n.getLatestResponse());

        n.hasResponded();
        long firstReponse = n.getLatestResponse();

        try { Thread.sleep(100); }
        catch (InterruptedException e) { e.printStackTrace(); assertTrue(false); }

        n.hasResponded();
        long secondResponse = n.getLatestResponse();
        assertTrue(firstReponse < secondResponse);
        assertEquals(0, n.getFailedResponses());
    }

    @Test
    void hasFailedToRespond() {
        RouterNode n = new RouterNode(new NodeID(), local, 55555);
        long firstReponse = n.getLatestResponse();

        n.hasFailedToRespond();
        long secondResponse = n.getLatestResponse();
        assertEquals(firstReponse, secondResponse);
        assertEquals(1, n.getFailedResponses());
    }

    @Test
    void getNode() {
        NodeID id = new NodeID();
        RouterNode n = new RouterNode(id, local, 55555);
        assertEquals(id, n.getNode());
    }

    @Test
    void getIPAddress() {
        RouterNode n = new RouterNode(new NodeID(), local, 55555);
        assertEquals(local, n.getIPAddress());
    }

    @Test
    void getPort() {
        RouterNode n = new RouterNode(new NodeID(), local, 55555);
        assertEquals(55555, n.getPort());
    }

    @Test
    void equals() {
        NodeID id = new NodeID();
        RouterNode n = new RouterNode(id, local, 55555);
        RouterNode n2 = new RouterNode(id, local, 55555);
        assertEquals(n, n2);

        assertNotEquals(new Object(), n);
        assertNotEquals(new Integer(1), n);
    }

    @Test
    void compare() {
        // Different
        RouterNode n = new RouterNode(new NodeID(), local, 55555);
        RouterNode n2 = new RouterNode(new NodeID(), local, 55555);

        n.hasResponded(); // Thus n should be > n2
        assertTrue(n.compareTo(n2) > 0);

        // Same
        assertTrue(n.compareTo(n) == 0);
    }

    @Test
    void isFresh() {
        RouterNode n = new RouterNode(new NodeID(), local, 55555);
        assertFalse(n.isFresh());

        n.hasResponded();
        assertTrue(n.isFresh());
    }

}