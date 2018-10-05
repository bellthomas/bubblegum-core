package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.exceptions.AddressInitialisationException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.social.SocialIdentity;

import javax.xml.soap.Node;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

public class Bubblegum {

    private InetAddress ipAddress;
    private SocialIdentity socialIdentity;

    public Bubblegum() {
        try {
            this.initialiseIPAddress();
            this.initialiseSocialIdentity();

            this.run();

        } catch (AddressInitialisationException e) {
            System.out.println("Failed to start network");
        }
    }

    private void run() {

        int numberOfNodes = 20;
        BubblegumNode[] nodes = new BubblegumNode[numberOfNodes];
        for(int i = 0; i < numberOfNodes; i++) nodes[i] = BubblegumNode.construct(this.socialIdentity, this.ipAddress, 44000 + i);
        System.out.println();

        // Bootstrap all nodes to node 0
        for(int i = 1; i < numberOfNodes; i++) {
            System.out.println("--------------------------");
            System.out.println("Node " + i);
            System.out.println();
            nodes[i].bootstrap(this.ipAddress, 44000);
            System.out.println("--------------------------");
            System.out.println();
            System.out.println();
        }

//        int numberOfNodes = 5000;
//        BubblegumNode[] nodes = new BubblegumNode[numberOfNodes];
//        for(int i = 0; i < nodes.length; i++) {
//            nodes[i] = BubblegumNode.construct(this.socialIdentity, this.ipAddress);
////            me.bootstrap(nodes[i]);
//            if(i % 1000 == 0) System.out.println(i);
//        }
//
//        NodeID search = new NodeID();
//        BigInteger b = new BigInteger(1, search.getKey());
//
//        Set<RouterNode> r = me.getNodesClosestToKey(search, 5);
//        for(RouterNode n : r) {
//            System.out.println("Distance [" + search.toString() + " -> " + n.getNode().toString() + "]: ");
//            System.out.println(new BigInteger(1, search.xorDistance(n.getNode())).abs());
//            System.out.println();
//        }

        System.out.println("Bootstrapping completed.");
        System.out.println();
        while(true) {

        }


        /**
         *
         * https://stackoverflow.com/questions/19329682/adding-new-nodes-to-kademlia-building-kademlia-routing-tables
         *
         * http://gleamly.com/article/introduction-kademlia-dht-how-it-works
         *
         * PING probes a node to see if it’s online.
         *
         * STORE instructs a node to store a [key, value] pair for later retrieval
         *
         * FIND NODE takes a 160-bit key as an argument, the recipient of the FIND_NODE RPC returns information for the k nodes closest to the target id.
         *
         * FIND VALUE behaves like FIND_NODE returning the k nodes closest to the target Identifier with one exception – if the RPC recipient has received a STORE for the key, it just returns the stored value
         *
         */
    }

    private void initialiseIPAddress() throws AddressInitialisationException {
        try {
            this.ipAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new AddressInitialisationException();
        }
    }

    private void initialiseSocialIdentity() {
        this.socialIdentity = new SocialIdentity();
    }

    public static void main(String[] args) {
        new Bubblegum();
    }

}
