package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.exceptions.AddressInitialisationException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.social.SocialIdentity;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

        int numberOfNodes = 50000;
        BubblegumNode[] nodes = new BubblegumNode[numberOfNodes];
        for(int i = 0; i < nodes.length; i++) {
            nodes[i] = BubblegumNode.construct(this.socialIdentity, this.ipAddress);
            me.bootstrap(nodes[i]);
            if(i % 1000 == 0) System.out.println(i);
        }

        me.printBuckets();

//        BubblegumNode node1 = BubblegumNode.construct(this.socialIdentity, this.ipAddress);
//        BubblegumNode node2 = BubblegumNode.construct(this.socialIdentity, this.ipAddress, "FDEC9F6A63BE7A02DB92CCF9663D15A24662F403", 2000);
//        node1.bootstrap(node2);
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
