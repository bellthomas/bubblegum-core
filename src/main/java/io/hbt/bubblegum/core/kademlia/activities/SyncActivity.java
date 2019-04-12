package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.auxiliary.ProtobufHelper;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServerWorker;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaSync.KademliaSync;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.function.Consumer;

public class SyncActivity extends NetworkActivity {

    private String networkID, foreignRecipient;
    private KademliaMessage originalSync;

    public SyncActivity(BubblegumNode self, RouterNode to) {
        this(self, to, self.getNetworkIdentifier());
    }

    public SyncActivity(BubblegumNode self, RouterNode to, String foreignRecipient) {
        super(self, to);
        this.networkID = self.getNetworkIdentifier();
        this.foreignRecipient = foreignRecipient;
    }


    public void setResponse(KademliaMessage originalSync) {
        super.setResponse(originalSync.getExchangeID());
        if(originalSync.hasSyncMessage() && originalSync.getSyncMessage().getStage() == 1) {
            this.originalSync = originalSync;
        } else {
            this.aborted = true;
        }
    }


    @Override
    public void run() {
        super.run();
        if(this.aborted) {
            this.onFail();
            return;
        }

        // Get real RouterNode if we have one
        RouterNode d = this.routingTable.getRouterNodeForID(this.to.getNode());
        if(d == null) d = this.to;
        final RouterNode destination = d;

        // dest and to with different ip/port?

        KademliaMessage message = null;
        if(!this.isResponse) {
            // Send stage 1
            message = ProtobufHelper.buildSyncMessage(
                this.localNode, destination, this.exchangeID,
                1, "", new byte[4]
            );
        }
        else {
            // Send stage 2.

            // Get A's public key.
            // Also implicitly checks the IP address for MITM.
            boolean valid = this.localNode.ensurePGPKeyIsLocal(
                originalSync.getSyncMessage().getLabel(),
                KademliaServerWorker.kademliaMessagesToPGPID(originalSync)
            );

            if(!valid) {
                // Suspected MITM.
                this.onFail();
                return;
            }

            message = ProtobufHelper.buildSyncMessage(
                this.localNode, destination, this.exchangeID,
                2, "", new byte[4]
            );
        }


        Consumer<KademliaMessage> response = this.isResponse ? (kademliaMessage -> {
            // Received stage one, sent stage two, and now receiving stage 3.
            if(kademliaMessage.getSyncMessage().getStage() == 3) {
                // validate
                this.onSuccess();
            } else {
                this.onFail();
            }

        }) : (kademliaMessage -> {
            // Sent stage one, received stage two, now sending stage 3.
            if(kademliaMessage.getSyncMessage().getStage() == 2) {

                // Get B's public key.
                // Also implicitly checks the IP address for MITM.
                boolean valid = this.localNode.ensurePGPKeyIsLocal(
                    "...fill in...",
                    KademliaServerWorker.kademliaMessagesToPGPID(kademliaMessage)
                );

                if(!valid) {
                    // Suspected MITM.
                    this.onFail();
                    return;
                }

                KademliaMessage stage3 = ProtobufHelper.buildSyncMessage(
                    this.localNode,
                    destination,
                    this.exchangeID,
                    3,
                    "",
                    new byte[4]
                );

                this.server.sendDatagram(localNode, destination, stage3, null);
                this.onSuccess();
            } else {
                this.onFail();
            }
        });

//        final NodeID destinationID = destination.getNode();
//        Runnable onTimeout = () -> {
//            RouterNode responder = this.routingTable.getRouterNodeForID(destinationID);
//            if(responder != null) responder.hasFailedToRespond();
//        };


        this.server.sendDatagram(localNode, destination, message, response);
        this.timeoutOnComplete();
    }

    public String getNetworkID() {
        return this.networkID;
    }

}
