package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.exceptions.BubblegumException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.activities.FindNodeActivity;
import io.hbt.bubblegum.core.kademlia.activities.PingActivity;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaNode.KademliaNode;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;
import io.hbt.bubblegum.core.social.SocialIdentity;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

    private BubblegumNode(SocialIdentity socialIdentity, InetAddress address, NodeID id, int port) {
        this.socialIdentity = socialIdentity;
        this.identifier = id;
        this.routingTable = new RoutingTable(this);

        try {
            // This is the node for a particular network
            this.server = new KademliaServer(this, port);
        } catch (BubblegumException e) {
            e.printStackTrace();
        }

        System.out.println("Constructed BubblegumNode: " + this.identifier.toString());
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


    public NodeID getIdentifier() {
        return this.identifier;
    }

    @Override
    public String toString() {
        return identifier.toString();
    }

    public boolean bootstrap(InetAddress address, int port) {
        // grpc call here, sync?

        System.out.println("["+this.server.getPort()+"] Starting bootstrapping process...  ("+address.getHostAddress()+":"+port+")");
        RouterNode to = new RouterNode(new NodeID(), address, port);
        PingActivity connection = new PingActivity(this.server, this, to, this.getRoutingTable());
        connection.run();
        System.out.println();

        if(connection.getComplete()) {
            // Was a success, now bootstrapped. getNodes from bootstrapped node
            FindNodeActivity findNodes = new FindNodeActivity(this.server, this, to, this.getRoutingTable(), this.identifier.toString());
            findNodes.run();

            if(findNodes.getComplete()) {
                Set<KademliaNode> foundNodes = findNodes.getResults();
                for(KademliaNode node : foundNodes) {
                    try {
                        RouterNode routerNode = new RouterNode(
                                new NodeID(node.getHash()),
                                InetAddress.getByName(node.getIpAddress()),
                                node.getPort()
                        );

                        System.out.println();
                        PingActivity nodePing = new PingActivity(this.server, this, routerNode, this.getRoutingTable());
                        nodePing.run();

                    } catch (MalformedKeyException e) {
                        e.printStackTrace();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
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

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof BubblegumNode) {
            if(obj == this) return true;
            else return this.identifier.equals(((BubblegumNode)obj).getIdentifier());
        }
        return false;
    }
}
