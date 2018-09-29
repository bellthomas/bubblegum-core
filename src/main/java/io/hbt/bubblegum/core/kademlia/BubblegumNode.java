package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.Bubblegum;
import io.hbt.bubblegum.core.exceptions.AddressInitialisationException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;
import io.hbt.bubblegum.core.social.SocialIdentity;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class BubblegumNode {
    private SocialIdentity socialIdentity;
    private NodeID identifier;
    private InetAddress ipAddress;
    private int port; // -1 signifies it will be assign randomly on server start
    private RoutingTable routingTable;

    private BubblegumNode(SocialIdentity socialIdentity, InetAddress address, NodeID id, int port) {
        this.socialIdentity = socialIdentity;
        this.port = port;
        this.identifier = id;
        this.ipAddress = address;
        this.routingTable = new RoutingTable(this);

//        System.out.println("Constructed BubblegumNode: " + this.identifier.toString());
    }

    public static BubblegumNode construct(SocialIdentity socialIdentity, InetAddress address) {
        return new BubblegumNode(socialIdentity, address, new NodeID(), -1);
    }

    public static BubblegumNode construct(SocialIdentity socialIdentity, InetAddress address, int port) {
        return new BubblegumNode(socialIdentity, address, new NodeID(), port);
    }

    public static BubblegumNode construct(SocialIdentity socialIdentity, InetAddress address, String id, int port) {
        NodeID nodeID = new NodeID();
        try {
            nodeID = new NodeID(id);
        } catch (MalformedKeyException e) {
            System.out.println("Malformed Key (" + id + "), generated a new one (" + nodeID.toString() + ")");
        } finally {
            return new BubblegumNode(socialIdentity, address, nodeID, port);
        }
    }

    public void bootstrap(BubblegumNode node) {
//        System.out.println("bootstrapping onto " + node.toString());
        this.routingTable.insert(new RouterNode(node));
    }


    public NodeID getIdentifier() {
        return this.identifier;
    }

    public void printBuckets() {
        this.routingTable.printBuckets();
    }

    @Override
    public String toString() {
        return identifier.toString();
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
