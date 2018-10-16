package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.auxiliary.logging.LoggingManager;
import io.hbt.bubblegum.core.exceptions.AddressInitialisationException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;
import io.hbt.bubblegum.core.kademlia.activities.LookupActivity;
import io.hbt.bubblegum.core.social.SocialIdentity;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

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

//            byte[] _0 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _1 = NodeID.hexToBytes("0483370C12CBA200");
//            byte[] _2 = NodeID.hexToBytes("2A927F4DB67395B2");
//            byte[] _3 = NodeID.hexToBytes("8CBB717EE01D2E61");
//            byte[] _4 = NodeID.hexToBytes("17D2468C055D8801");
//            byte[] _5 = NodeID.hexToBytes("D35CB694A4E49834");
//            byte[] _6 = NodeID.hexToBytes("FEC8653833B4B47B");
//            byte[] _7 = NodeID.hexToBytes("64DB696D59B254D3");
//            byte[] _8 = NodeID.hexToBytes("8D627E4B136AD453");
//            byte[] _9 = NodeID.hexToBytes("F719300239113613");
//            byte[] _10 = NodeID.hexToBytes("4AA4B16679F9E946");
//            byte[] _11 = NodeID.hexToBytes("25400140FCF1A1F6");
//            byte[] _12 = NodeID.hexToBytes("60F5236B1D84BCFF");
//            byte[] _13 = NodeID.hexToBytes("AE1618F31BABDB0B");
//            byte[] _14 = NodeID.hexToBytes("26D516713019D5FF");
//            byte[] _15 = NodeID.hexToBytes("FE121D7591E2D988");
//            byte[] _16 = NodeID.hexToBytes("9D8B3DA15C34C53D");
//            byte[] _17 = NodeID.hexToBytes("E482912572C87033");
//            byte[] _18 = NodeID.hexToBytes("9D64EE2FA2568895");
//            byte[] _19 = NodeID.hexToBytes("116583F180CEF027");
//            byte[] _20 = NodeID.hexToBytes("77D62D442B0FED63");
//            byte[] _21 = NodeID.hexToBytes("A5A8B7CE4E378626");
//            byte[] _22 = NodeID.hexToBytes("D9D92F5ACED26F8F");
//            byte[] _23 = NodeID.hexToBytes("EBD3C7248BBF4C5B");
//            byte[] _24 = NodeID.hexToBytes("98B5D2567459F646");
//            byte[] _25 = NodeID.hexToBytes("D15DF9DC244F4FAF");
//            byte[] _26 = NodeID.hexToBytes("0DD2444ABC385433");
//            byte[] _27 = NodeID.hexToBytes("680236CC0051A27F");
//            byte[] _28 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _29 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _30 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _31 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _32 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _33 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _34 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _35 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _36 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _37 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _38 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _39 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _40 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _41 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _42 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _43 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _44 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _45 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _46 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _47 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _48 = NodeID.hexToBytes("5381D7B9CECA6B7A");
//            byte[] _49 = NodeID.hexToBytes("5381D7B9CECA6B7A");


            this.run();

        } catch (AddressInitialisationException e) {
            System.out.println("Failed to start network");
        }
    }

    private void run() {

        int numberOfNodes = 200;
        ActivityExecutionContext context = new ActivityExecutionContext(numberOfNodes);
        LoggingManager loggingManager = LoggingManager.getInstance();

        BubblegumNode[] nodes = new BubblegumNode[numberOfNodes];
        for(int i = 0; i < numberOfNodes; i++) {
            nodes[i] = BubblegumNode.construct(this.socialIdentity, context, loggingManager.getLogger(i), this.ipAddress, 44000 + i);
        }
        System.out.println();

        // Bootstrap all nodes to node 0
        for(int i = 1; i < numberOfNodes; i++) {
            System.out.println(i);
            nodes[i].bootstrap(this.ipAddress, 44000);
        }

        System.out.println("Initial network built.\n");

        BubblegumNode newcomer = BubblegumNode.construct(this.socialIdentity, context, loggingManager.getLogger(numberOfNodes), this.ipAddress, 44000 + numberOfNodes);
        try {
            Thread.sleep(1000);
            loggingManager.getLogger(numberOfNodes).logMessage("Starting bootstrap");
            newcomer.bootstrap(this.ipAddress, 44000);

            System.out.println("Starting value lookup:");
            LookupActivity getval = new LookupActivity(newcomer, nodes[0].getIdentifier(), 5, true);
            getval.run();

            if(getval.getComplete() && getval.getSuccess()) {
                System.out.println("Success.");
                if(getval.getResult() != null && getval.getResult().length > 0) {
                    System.out.println(Arrays.toString(getval.getResult()));
                }
                else {
                    System.out.println("No result...");
                }
            }
            else {
                System.out.println("Failed.");
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        NodeID closest = null;
        for(BubblegumNode bn : nodes) {
            if(closest == null) closest = bn.getIdentifier();
            else {
                byte[] o1Distance = newcomer.getIdentifier().xorDistance(bn.getIdentifier());
                byte[] o2Distance = newcomer.getIdentifier().xorDistance(closest);
                if(new BigInteger(1, o1Distance).abs().compareTo(new BigInteger(1, o2Distance).abs()) < 0) {
                    // o1 bigger than o2
//                    System.out.println("New closest");
//                    System.out.println(new BigInteger(1, o1Distance).abs());
//                    System.out.println("smaller than ");
//                    System.out.println(new BigInteger(1, o2Distance).abs());
                    closest = bn.getIdentifier();
                }
            }
        }
        System.out.println("Closest is " + closest.toString());

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
