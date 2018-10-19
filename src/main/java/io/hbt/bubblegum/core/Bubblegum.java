package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.auxiliary.logging.LoggingManager;
import io.hbt.bubblegum.core.exceptions.AddressInitialisationException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;
import io.hbt.bubblegum.core.kademlia.activities.LookupActivity;
import io.hbt.bubblegum.core.kademlia.activities.PrimitiveStoreActivity;
import io.hbt.bubblegum.core.kademlia.activities.StoreActivity;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.social.SocialIdentity;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;

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

        int numberOfNodes = 200;
        ActivityExecutionContext context = new ActivityExecutionContext(numberOfNodes);
        LoggingManager loggingManager = LoggingManager.getInstance();

        BubblegumNode[] nodes = new BubblegumNode[numberOfNodes];
        for(int i = 0; i < numberOfNodes; i++) {
            nodes[i] = BubblegumNode.construct(this.socialIdentity, context, loggingManager.getLogger(i), this.ipAddress, 44000 + i);
        }
        System.out.println();

//         Bootstrap all nodes to node 0
        for(int i = 1; i < numberOfNodes; i++) {
            System.out.println(i);
            nodes[i].bootstrap(this.ipAddress, 44000);
        }

        System.out.println("Initial network built.\n");

        BubblegumNode newcomer = BubblegumNode.construct(this.socialIdentity, context, loggingManager.getLogger(numberOfNodes), this.ipAddress, 44000 + numberOfNodes);
        try {
            Thread.sleep(1000);
            loggingManager.getLogger(numberOfNodes).logMessage("Starting bootstrap");
            newcomer.bootstrap(this.ipAddress, 44034);

            RouterNode node0 = new RouterNode(
                    new NodeID(nodes[0].getIdentifier().toString()),
                    nodes[0].getServer().getLocal(),
                    nodes[0].getServer().getPort()
            );

            byte[] b = new byte[20];
            new Random().nextBytes(b);
            System.out.println("Saving payload: " + Arrays.toString(b));

            NodeID storeKey = new NodeID();

            StoreActivity storeActivity = new StoreActivity(newcomer, storeKey.toString(), b);
//            PrimitiveStoreActivity storeActivity = new PrimitiveStoreActivity(newcomer, node0, nodes[0].getIdentifier().toString(), b);
            storeActivity.run();



            System.out.println("Starting value lookup:");
            LookupActivity getval = new LookupActivity(newcomer, storeKey, 5, true);
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
        catch (MalformedKeyException e) {
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
