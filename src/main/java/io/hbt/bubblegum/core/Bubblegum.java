package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.exceptions.AddressInitialisationException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
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

        BubblegumNode me = BubblegumNode.construct(this.socialIdentity, this.ipAddress);

        int numberOfNodes = 5000;
        BubblegumNode[] nodes = new BubblegumNode[numberOfNodes];
        for(int i = 0; i < nodes.length; i++) {
            nodes[i] = BubblegumNode.construct(this.socialIdentity, this.ipAddress);
            me.bootstrap(nodes[i]);
            if(i % 1000 == 0) System.out.println(i);
        }

        NodeID search = new NodeID();
        BigInteger b = new BigInteger(1, search.getKey());

        Set<BubblegumNode> r = me.getNodesClosestToKey(search, 5);
        for(BubblegumNode n : r) {
            System.out.println("Distance [" + search.toString() + " -> " + n.getIdentifier().toString() + "]: ");
            System.out.println(new BigInteger(1, search.xorDistance(n.getIdentifier())).abs());
            System.out.println();
        }

        System.out.println("");
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
