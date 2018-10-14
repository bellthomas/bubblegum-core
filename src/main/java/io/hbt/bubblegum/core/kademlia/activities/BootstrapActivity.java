package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServer;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaNode;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;

import java.util.Set;

public class BootstrapActivity extends NetworkActivity {

    public BootstrapActivity(KademliaServer server, BubblegumNode self, RouterNode to, RoutingTable routingTable) {
        super(server, self, to, routingTable);
    }


    @Override
    public void run() {
        // Ping
        this.print("Starting bootstrapping process...  ("+this.to.getIPAddress().getHostAddress()+":"+this.to.getPort()+")");
        PingActivity ping = new PingActivity(this.server, this.localNode, this.to, this.routingTable);
        ping.run();

        if(ping.getComplete()) {
            // Was a success, now bootstrapped. getNodes from bootstrapped node
            FindNodeActivity findNodes = new FindNodeActivity(this.server, this.localNode, this.to, this.routingTable, this.localNode.getIdentifier().toString());
            findNodes.run();

            if(findNodes.getComplete()) {
                Set<BgKademliaNode.KademliaNode> foundNodes = findNodes.getResults();
                for(BgKademliaNode.KademliaNode node : foundNodes) {
                    RouterNode routerNode = RouterNode.fromKademliaNode(node);
                    if(routerNode != null) {
                        RouterNode destination = this.routingTable.getRouterNodeForID(this.to.getNode());
                        if(destination == null || System.currentTimeMillis() - destination.getLatestResponse() > 600000000000L) {
                            PingActivity nodePing = new PingActivity(this.server, this.localNode, routerNode, this.routingTable);
                            this.localNode.getExecutionContext().addPingActivity(this.localNode.getIdentifier().toString(), nodePing);
                        }
                    }
                }

                this.localNode.getExecutionContext().addDelayedActivity(
                        this.localNode.getIdentifier().toString(),
                        () -> this.localNode.getRoutingTable().refreshBuckets(),
                        10000
                );

                this.complete = true;
            }

        }

        else {
            this.print("No response from bootstrap node.");
        }
    }
}
