package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.auxiliary.ProtobufHelper;
import io.hbt.bubblegum.core.databasing.Database;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServerWorker;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaBinaryPayload.KademliaBinaryPayload;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

public class PrimitiveStoreActivity extends NetworkActivity {

    private byte[] payload;
    private String key;

    public PrimitiveStoreActivity(BubblegumNode localNode, RouterNode to, String key, byte[] payload) {
        super(localNode, to);
        this.payload = payload;
        this.key = key;
    }

    @Override
    public void run() {
        super.run();
        if(this.aborted) {
            this.onFail();
            return;
        }

        if(this.isResponse) {
            this.print("Responding to STORE("+this.key+") request to " + this.to.getNode().toString());
            boolean accepted = Database.getInstance().add(this.localNode.getIdentifier(), this.key, this.payload);
            KademliaMessage message = ProtobufHelper.buildStoreResponse(this.localNode, this.to, this.exchangeID, accepted);
            this.server.sendDatagram(this.localNode, this.to, message, null);
            this.onSuccess();
        }
        else {
            this.print("Sending STORE("+this.key+") request to " + this.to.getNode().toString());
            KademliaMessage message = ProtobufHelper.buildStoreRequest(this.localNode, this.to, this.exchangeID, this.key, this.payload);
            this.server.sendDatagram(this.localNode, this.to, message, (kademliaMessage -> {
                KademliaBinaryPayload payload = KademliaServerWorker.extractPayload(kademliaMessage);
                if(payload != null) {
                    if (payload.hasStoreResponse()) {
                        if (payload.getStoreResponse().getAccepted()) {
                            this.onSuccess("STORE(" + this.key + ") operation successful to " + this.to.getNode().toString());
                        } else {
                            this.onFail("STORE(" + this.key + ") operation failed to " + this.to.getNode().toString());
                        }
                    } else {
                        this.onFail("Invalid response to STORE (" + this.key + ")request from " + this.to.getNode().toString());
                    }
                } else {
                    this.onFail();
                }
            }));
        }

        this.timeoutOnComplete();
    }
}
