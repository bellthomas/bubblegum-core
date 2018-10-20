package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaPing.KademliaPing;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Consumer;

public class PingActivity extends NetworkActivity {

    private String networkID;

    public PingActivity(BubblegumNode self, RouterNode to) {
        super(self, to);
        this.networkID = self.getNetworkIdentifier();
    }

    @Override
    public void run() {

        // Get real RouterNode if we have one
        RouterNode destination = this.routingTable.getRouterNodeForID(this.to.getNode());
        if(destination == null) {
            destination = this.to;
            if(!isResponse) this.print("-------- No node in routing table");
            else this.print("-------- Must reply to pings");
        }
        else {
            if (!isResponse) this.print("-------- Found node in routing table, fresh: " + destination.isFresh());
            else this.print("-------- Must reply to pings");
        }

        // dest and to with different ip/port?


        if(this.isResponse) {
            this.print("["+this.exchangeID+"] Replying to PING from " + destination.getIPAddress().getHostAddress() + ":" + destination.getPort());
        }
        else if(destination.isFresh()) {
            this.onSuccess("Node fresh, PING not required for " + destination.getIPAddress().getHostAddress() + ":" + destination.getPort());
            return;
        }
        else {
            this.print("Starting PING to " + destination.getIPAddress().getHostAddress() + ":" + destination.getPort());
        }


        KademliaMessage.Builder message = KademliaMessage.newBuilder();
        KademliaPing.Builder connectionMessage = KademliaPing.newBuilder();
        message.setOriginHash(this.localNode.getNodeIdentifier().toString());
        message.setOriginIP(this.server.getLocal().getHostAddress());
        message.setOriginPort(this.server.getPort());
        message.setExchangeID(this.exchangeID);
        connectionMessage.setReply(this.isResponse);
        if(this.isResponse) connectionMessage.setNetworkID(this.localNode.getNetworkIdentifier());
        message.setPingMessage(connectionMessage.build());

        Consumer<KademliaMessage> response = this.isResponse ? null : (kademliaMessage -> {
            // TODO verify ip/port
            try {
                RouterNode responder = this.routingTable.getRouterNodeForID(new NodeID(kademliaMessage.getOriginHash()));
                if(responder == null) responder = new RouterNode(
                        new NodeID(kademliaMessage.getOriginHash()),
                        InetAddress.getByName(kademliaMessage.getOriginIP()),
                        kademliaMessage.getOriginPort()
                );

                // Get the returned network identifier
                String newNetworkID = kademliaMessage.getPingMessage().getNetworkID();
                if(newNetworkID != null && newNetworkID.length() > 0) this.networkID = newNetworkID;

                responder.hasResponded();
                this.routingTable.insert(responder);
                this.onSuccess("PING response from " + responder.getNode().toString());

            } catch (MalformedKeyException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

        });

//        final NodeID destinationID = destination.getNode();
//        Runnable onTimeout = () -> {
//            RouterNode responder = this.routingTable.getRouterNodeForID(destinationID);
//            if(responder != null) responder.hasFailedToRespond();
//        };

        this.server.sendDatagram(destination, message.build(), response);
        if(!this.isResponse) this.timeoutOnComplete();
    }

    public String getNetworkID() {
        return this.networkID;
    }

}
