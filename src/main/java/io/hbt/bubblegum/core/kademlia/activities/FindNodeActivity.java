package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServer;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;

public class FindNodeActivity extends NetworkActivity {

    private final String search;
    public FindNodeActivity(KademliaServer server, BubblegumNode self, RouterNode to, RoutingTable routingTable, String search) {
        super(server, self, to, routingTable);
        this.search = search;
    }

    @Override
    public void run() {
        if(this.isResponse) {
            this.print("Replying to FIND_NODE from " + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());
        }
        else {
            this.print("Starting FIND_NODE(" + this.search + ") activity to " + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());
        }
    }
}
