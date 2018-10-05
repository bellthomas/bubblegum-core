package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServer;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
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
        if(this.isResponse) {
            this.print("Replying to ping from " + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());
        }
        else {
            this.print("Starting ping to " + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());
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
                RouterNode responder = new RouterNode(
                        new NodeID(kademliaMessage.getOriginHash()),
                        InetAddress.getByName(kademliaMessage.getOriginIP()),
                        kademliaMessage.getOriginPort()
                );

                this.routingTable.insert(responder);
                this.complete = true;

                this.print("Ping response from " + responder.getNode().toString());
            } catch (MalformedKeyException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

        });

        this.server.sendDatagram(this.to, message.build(), response);
        this.timeoutOnComplete();
    }

}
