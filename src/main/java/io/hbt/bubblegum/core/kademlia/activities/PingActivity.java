package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServer;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaPing.KademliaPing;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Consumer;

public class PingActivity extends NetworkActivity {

    public PingActivity(KademliaServer server, BubblegumNode self, RouterNode to, RoutingTable routingTable) {
        super(server, self, to, routingTable);
    }

    @Override
    public void run() {

        // Get real RouterNode if we have one
        RouterNode destination = this.routingTable.getRouterNodeForID(this.to.getNode());
        if(destination == null) destination = this.to;

        // dest and to with different ip/port?

        if(this.isResponse) {
            this.print("Replying to PING from " + destination.getIPAddress().getHostAddress() + ":" + destination.getPort());
        }
        else {

            if(System.nanoTime() - destination.getLatestResponse() < 600000000000L) {
                this.print("Node fresh, PING not required for " + destination.getIPAddress().getHostAddress() + ":" + destination.getPort());
                this.complete = true;
                return;
            }
            this.print("Starting PING to " + destination.getIPAddress().getHostAddress() + ":" + destination.getPort());
        }

        KademliaMessage.Builder message = KademliaMessage.newBuilder();
        KademliaPing.Builder connectionMessage = KademliaPing.newBuilder();
        message.setOriginHash(this.localNode.getIdentifier().toString());
        message.setOriginIP(this.server.getLocal().getHostAddress());
        message.setOriginPort(this.server.getPort());
        message.setExchangeID(this.exchangeID);
        message.setPingMessage(connectionMessage.build());

        Consumer<KademliaMessage> response = this.isResponse ? null : (kademliaMessage -> {

            // TODO verify ip/port
            try {
//                RouterNode responder = new RouterNode(
//                        new NodeID(kademliaMessage.getOriginHash()),
//                        InetAddress.getByName(kademliaMessage.getOriginIP()),
//                        kademliaMessage.getOriginPort()
//                );

                RouterNode responder = this.routingTable.getRouterNodeForID(new NodeID(kademliaMessage.getOriginHash()));
                if(responder == null) responder = new RouterNode(
                        new NodeID(kademliaMessage.getOriginHash()),
                        InetAddress.getByName(kademliaMessage.getOriginIP()),
                        kademliaMessage.getOriginPort()
                );

                this.routingTable.insert(responder);
                this.complete = true;

                this.print("PING response from " + responder.getNode().toString());
            } catch (MalformedKeyException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

        });

        this.server.sendDatagram(destination, message.build(), response);
        this.timeoutOnComplete();
    }

}
