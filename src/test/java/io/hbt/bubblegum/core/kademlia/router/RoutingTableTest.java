package io.hbt.bubblegum.core.kademlia.router;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class RoutingTableTest {

    static BubblegumNode localNode;
    static InetAddress localAddress;

    @BeforeAll
    static void setup() {
        localNode = new BubblegumNode.Builder()
//                .setSocialIdentity(new SocialIdentity())
                .setExecutionContext(new ActivityExecutionContext(1))
//                .setLogger(LoggingManager.getLogger("_tests"))
                .build();
        try { localAddress = InetAddress.getLocalHost(); }
        catch (UnknownHostException e) {
            assertTrue(false, "Address initialisation failed");
            return;
        }
    }

    @Test
    void insert() {
        RoutingTable routingTable = new RoutingTable(localNode);
        NodeID id = new NodeID();
        routingTable.insert(new RouterNode(id, localAddress, 55555));

        RouterBucket bucket = routingTable.getBucketForNode(id);
        assertEquals(1, bucket.getNodes().size());
        assertTrue(((RouterNode)(bucket.getNodes().toArray()[0])).getNode().equals(id));
    }

}