package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.auxiliary.ProtobufHelper;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.net.UnknownHostException;
import java.util.function.Consumer;

/**
 * Implementation of the PING RPC.
 */
public class PingActivity extends NetworkActivity {

    private String networkID, foreignRecipient;
    private KademliaMessage originalPing;

    /**
     * Constructor.
     * @param self the owning BubblegumNode.
     * @param to The peer being pinged.
     */
    public PingActivity(BubblegumNode self, RouterNode to) {
        this(self, to, self.getNetworkIdentifier());
    }

    /**
     * Constructor.
     * @param self The owning BubblegumNode.
     * @param to The peer being pinged.
     * @param foreignRecipient The peer's foreignRecipient key.
     */
    public PingActivity(BubblegumNode self, RouterNode to, String foreignRecipient) {
        super(self, to);
        this.networkID = self.getNetworkIdentifier();
        this.foreignRecipient = foreignRecipient;
    }

    /**
     * Declare that this activity was created in response to another message.
     * @param originalPing The original ping message.
     */
    public void setResponse(KademliaMessage originalPing) {
        super.setResponse(originalPing.getExchangeID());
        this.originalPing = originalPing;
    }

    /**
     * Run the RPC's logic.
     */
    @Override
    public void run() {
        super.run();
        if(this.aborted) {
            this.onFail();
            return;
        }

        // Get real RouterNode if we have one
        RouterNode destination = this.to;
        if(!Configuration.ENABLE_IMPERSONATION_WoT_PROTECTION || !Configuration.ENABLE_PGP) {
            destination = this.routingTable.getRouterNodeForID(this.to.getNode());
            if (destination == null) destination = this.to;
        }

        if(this.isResponse) {
            this.onSuccess("Replying to PING from " + this.foreignRecipient);
        }
        else if(destination.isFresh()) {
            this.onSuccess("Node fresh, PING not required for " + this.networkID + ":" + destination.getNode());
            return;
        }


        Consumer<KademliaMessage> response = this.isResponse ? null : (kademliaMessage -> {
            try {
                RouterNode responder = this.routingTable.getRouterNodeForID(new NodeID(kademliaMessage.getOriginHash()));
                if(responder == null) responder = new RouterNode(
                    new NodeID(kademliaMessage.getOriginHash()),
                    NetworkingHelper.getInetAddress(kademliaMessage.getOriginIP()),
                    kademliaMessage.getOriginPort()
                );

                // Get the returned network identifier
                String newNetworkID = kademliaMessage.getOriginNetwork();
                if(newNetworkID != null && newNetworkID.length() > 0) this.networkID = newNetworkID;

                responder.hasResponded();
                if(!Configuration.ENABLE_IMPERSONATION_WoT_PROTECTION || !Configuration.ENABLE_PGP) {
                    this.routingTable.insert(responder);
                }
                this.originalPing = kademliaMessage;
                this.onSuccess("PING response from " + responder.getNode().toString());

            } catch (MalformedKeyException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

        });

        String originHash = (this.originalPing == null) ? this.to.getNode().toString() : this.originalPing.getOriginHash();

        this.server.sendDatagram(
            localNode,
            destination,
            ProtobufHelper.buildPingMessage(this.localNode, originHash, this.exchangeID, this.isResponse, this.foreignRecipient),
            response
        );

        if(!this.isResponse) this.timeoutOnComplete();
    }

    /**
     * Retrieve the networkIdentifier reported by the remote node.
     * @return the networkIdentifier.
     */
    public String getNetworkID() {
        return this.networkID;
    }

    /**
     * Retrieve the original ping message if a response activity.
     * @return The original ping.
     */
    public KademliaMessage getPing() {
        return this.originalPing;
    }

} // end PingActivity class
