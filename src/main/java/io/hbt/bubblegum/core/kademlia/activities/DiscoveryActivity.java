package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.auxiliary.ProtobufHelper;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.HashSet;
import java.util.Set;

public class DiscoveryActivity extends NetworkActivity {

    private Set<String> entries;
    private String foriegnRecipient;
    public DiscoveryActivity(BubblegumNode localNode, RouterNode to) {
        super(localNode, to);
    }

    public void setResponse(String responseID, Set<String> entries, String foreignRecipient) {
        super.setResponse(responseID);
        this.entries = entries;
        this.foriegnRecipient = foreignRecipient;
    }

    @Override
    public void run() {
        super.run();
        if(this.aborted) {
            this.onFail();
            return;
        }

        if(this.isResponse) {
            KademliaMessage message = ProtobufHelper.buildDiscoveryResponse(this.localNode, this.to, this.exchangeID, this.entries, this.foriegnRecipient);
            this.server.sendDatagram(this.localNode, this.to, message, null);
            this.onSuccess();
        }
        else {
            KademliaMessage message = ProtobufHelper.buildDiscoveryRequest(this.localNode, this.to, this.exchangeID);
            this.server.sendDatagram(this.localNode, this.to, message, (kademliaMessage -> {
                if(kademliaMessage.hasDiscoveryResponse()) {
                    this.entries = new HashSet<>();
                    for(String entry : kademliaMessage.getDiscoveryResponse().getRecipientsList()) {
                        if(!entry.equals(this.localNode.getRecipientID())) this.entries.add(entry);
                    }
                    this.onSuccess();
                }
                else {
                    this.onFail("Invalid response to DiscoveryRequest");
                }
            }));
            this.timeoutOnComplete();
        }
    }

    public Set<String> getEntries() {
        return this.entries;
    }
}
