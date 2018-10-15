package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.auxiliary.logging.LoggingManager;
import io.hbt.bubblegum.core.exceptions.AddressInitialisationException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;
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


//            NodeID n = new NodeID();
//
//            for(int i = 0; i < NodeID.KEY_BIT_LENGTH; i++) {
//                System.out.println(n + " --- " + n.getKeyBitsString());
//                NodeID generated = n.generateIDWithSharedPrefixLength(i);
//                System.out.println(generated.toString() + " --- " + generated.getKeyBitsString());
//
//                System.out.println(i + " - " + n.sharedPrefixLength(generated));
//                System.out.println();
//            }


            this.run();

        } catch (AddressInitialisationException e) {
            System.out.println("Failed to start network");
        }
    }

    private void run() {

        int numberOfNodes = 10;
        ActivityExecutionContext context = new ActivityExecutionContext(numberOfNodes);
        LoggingManager loggingManager = LoggingManager.getInstance();

        BubblegumNode[] nodes = new BubblegumNode[numberOfNodes];
        for(int i = 0; i < numberOfNodes; i++) {
            nodes[i] = BubblegumNode.construct(this.socialIdentity, context, loggingManager.getLogger(i), this.ipAddress, 44000 + i);
        }
        System.out.println();

        // Bootstrap all nodes to node 0
        for(int i = 1; i < numberOfNodes; i++) {
            nodes[i].bootstrap(this.ipAddress, 44000);
        }

        System.out.println("Initial network built.");

        BubblegumNode newcomer = BubblegumNode.construct(this.socialIdentity, context, loggingManager.getLogger(numberOfNodes), this.ipAddress, 44000 + numberOfNodes);
        try {
            Thread.sleep(1000);
            loggingManager.getLogger(numberOfNodes).logMessage("Starting bootstrap");
            newcomer.bootstrap(this.ipAddress, 44002);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
