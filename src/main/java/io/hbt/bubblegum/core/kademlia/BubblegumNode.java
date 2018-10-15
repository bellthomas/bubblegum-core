package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.auxiliary.logging.Logger;
import io.hbt.bubblegum.core.exceptions.BubblegumException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.activities.*;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;
import io.hbt.bubblegum.core.social.Database;
import io.hbt.bubblegum.core.social.SocialIdentity;

import java.net.InetAddress;
import java.util.Set;

/**
 * This is local node
 * One per social network a part of
 */
public class BubblegumNode {
    private SocialIdentity socialIdentity;
    private NodeID identifier;
    private RoutingTable routingTable;
    private KademliaServer server;
    private ActivityExecutionContext executionContext;
    private Database db;
    private Logger logger;

    private BubblegumNode(SocialIdentity socialIdentity, ActivityExecutionContext context, Logger logger, InetAddress address, NodeID id, int port) {
        this.socialIdentity = socialIdentity;
        this.identifier = id;
        this.routingTable = new RoutingTable(this);
        this.executionContext = context;
        this.logger = logger;
        this.db = new Database();

        try {
            // This is the node for a particular network
            this.server = new KademliaServer(this, port);
        } catch (BubblegumException e) {
            e.printStackTrace();
        }

        this.log("Constructed BubblegumNode: " + this.identifier.toString());
    }

    public static BubblegumNode construct(SocialIdentity socialIdentity, ActivityExecutionContext context, Logger logger, InetAddress address) {
        return new BubblegumNode(socialIdentity, context, logger, address, new NodeID(), -1);
    }

    public static BubblegumNode construct(SocialIdentity socialIdentity, ActivityExecutionContext context, Logger logger, InetAddress address, int port) {
        return new BubblegumNode(socialIdentity, context, logger, address, new NodeID(), port);
    }

    public static BubblegumNode construct(SocialIdentity socialIdentity, ActivityExecutionContext context, Logger logger, InetAddress address, String id, int port) {
        NodeID nodeID = new NodeID();
        try {
            nodeID = new NodeID(id);
        } catch (MalformedKeyException e) {
            logger.logMessage("Malformed Key (" + id + "), generated a new one (" + nodeID.toString() + ")");
        } finally {
            return new BubblegumNode(socialIdentity, context, logger, address, nodeID, port);
        }
    }


    public NodeID getIdentifier() {
        return this.identifier;
    }

    @Override
    public String toString() {
        return identifier.toString();
    }

    public boolean bootstrap(InetAddress address, int port) {

        RouterNode to = new RouterNode(new NodeID(), address, port);
        BootstrapActivity boostrapActivity = new BootstrapActivity(this.server, this, to, this.getRoutingTable());
        boostrapActivity.run(); // sync

//        this.log("Bootstrap Level 1 Completed");
//
//        Set<RouterNode> closest = this.routingTable.getNodesClosestToKey(this.getIdentifier(), 8);
//        for(RouterNode node : closest) {
//            this.executionContext.addActivity(this.identifier.toString(), new BootstrapActivity(this.server, this, node, this.getRoutingTable()));
//        }

        return boostrapActivity.getComplete(); // only success of first level calls

    }

    public Set<RouterNode> getNodesClosestToKey(NodeID node, int numToGet) {
        return this.routingTable.getNodesClosestToKey(node, numToGet);
    }

    public void printBuckets() {
        this.routingTable.printBuckets();
    }

    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    public ActivityExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    public KademliaServer getServer() {
        return this.server;
    }

    public void log(String message) {
        this.logger.logMessage(message);
    }

    public boolean databaseHasKey(String key) {
        return this.db.hasKey(key);
    }

    public byte[] databaseRetrieveValue(String key) {
        return this.db.valueForKey(key);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof BubblegumNode) {
            if(obj == this) return true;
            else return this.identifier.equals(((BubblegumNode)obj).getIdentifier());
        }
        return false;
    }
}
