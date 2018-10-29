package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.auxiliary.ProtobufHelper;
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

    private String networkID, foreignRecipient;
    private KademliaMessage originalPing;

    public PingActivity(BubblegumNode self, RouterNode to) {
        this(self, to, self.getNetworkIdentifier());
    }

    public PingActivity(BubblegumNode self, RouterNode to, String foreignRecipient) {
        super(self, to);
        this.networkID = self.getNetworkIdentifier();
        this.foreignRecipient = foreignRecipient;
    }


    public void setResponse(KademliaMessage originalPing) {
        super.setResponse(originalPing.getExchangeID());
        this.originalPing = originalPing;
    }


    @Override
    public void run() {

        // Get real RouterNode if we have one
        RouterNode destination = this.routingTable.getRouterNodeForID(this.to.getNode());
        if(destination == null) {
            destination = this.to;
//            if(!isResponse) this.print("-------- No node in routing table");
//            else this.print("-------- Must reply to pings");
        }
        else {
//            if (!isResponse) this.print("-------- Found node in routing table, fresh: " + destination.isFresh());
//            else this.print("-------- Must reply to pings");
        }

        // dest and to with different ip/port?


        if(this.isResponse) {
            this.print("["+this.exchangeID+"] Replying to PING from " + this.networkID + ":" + destination.getNode());
        }
        else if(destination.isFresh()) {
            this.onSuccess("Node fresh, PING not required for " + this.networkID + ":" + destination.getNode());
//            return;
        }
        else {
            this.print("Starting PING to " + this.networkID + ":" + destination.getNode());
        }


        Consumer<KademliaMessage> response = this.isResponse ? null : (kademliaMessage -> {
            // TODO Verify IP/Port
            try {
                RouterNode responder = this.routingTable.getRouterNodeForID(new NodeID(kademliaMessage.getOriginHash()));
                if(responder == null) responder = new RouterNode(
                    new NodeID(kademliaMessage.getOriginHash()),
                    InetAddress.getByName(kademliaMessage.getOriginIP()),
                    kademliaMessage.getOriginPort()
                );

                // Get the returned network identifier
                String newNetworkID = kademliaMessage.getOriginNetwork();
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

        String originHash = (this.originalPing == null) ? this.to.getNode().toString() : this.originalPing.getOriginHash();

        this.server.sendDatagram(
            localNode,
            destination,
            ProtobufHelper.buildPingMessage(this.localNode, originHash, this.exchangeID, this.isResponse, this.foreignRecipient),
            response
        );

        if(!this.isResponse) this.timeoutOnComplete();
    }

    public String getNetworkID() {
        return this.networkID;
    }

}
