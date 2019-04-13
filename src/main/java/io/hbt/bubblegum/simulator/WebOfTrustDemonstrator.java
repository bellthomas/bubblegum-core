package io.hbt.bubblegum.simulator;

import io.hbt.bubblegum.core.Bubblegum;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;

import java.util.List;

public class WebOfTrustDemonstrator {
    public static void main(String[] args) {

        System.out.println("Initialising scenario...");
        Bubblegum bubblegum = new Bubblegum(false);
        NetworkingHelper.setLookupExternalIP(false);
        NodeID id_a = new NodeID();
        NodeID id_b = new NodeID();
        NodeID id_c = new NodeID();

        BubblegumNode a = bubblegum.createNode(id_a);
        BubblegumNode b = bubblegum.createNode(id_b);
        BubblegumNode c = bubblegum.createNode(id_b);

        try {
            System.out.println("Sleeping for 5 seconds to ensure PGP keys have propagated.\n");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Bootstrapping B onto A... ");
        if(b.bootstrap(a.getServer().getLocal(), a.getServer().getPort(), a.getRecipientID())) {
            System.out.println("Bootstrap successful. Now to check everything is working, perform a test store and retrieval.");

            a.store(NodeID.hash("foo"), "bar".getBytes());
            List<byte[]> results = b.lookup(NodeID.hash("foo"));
            boolean found = false;
            for(byte[] value : results) {
                if("bar".equals(new String(value))) found = true;
            }

            if(found) {
                System.out.println("Store and Retrieve successful.\n");
                System.out.println("Now bootstrapping C to A");

                if(!c.bootstrap(a.getServer().getLocal(), a.getServer().getPort(), a.getRecipientID())) {
                    System.out.println("C rejected as expected. Try again to test cache...\n");
                    if(!c.bootstrap(a.getServer().getLocal(), a.getServer().getPort(), a.getRecipientID())) {
                        System.out.println("C rejected as expected. Try on B...");
                        if(!c.bootstrap(b.getServer().getLocal(), b.getServer().getPort(), b.getRecipientID())) {
                            System.out.println("B rejected as expected.");
                            System.exit(0);
                        }
                    }
                } else {
                    System.out.println("C bootstrapped successfully.");
                    System.exit(0);
                }

            }
        }

        System.out.println("An error occurred.");
        System.exit(0);
    }
}
