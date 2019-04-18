package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServerWorker;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.Set;
import java.util.function.Consumer;

public class BootstrapActivity extends NetworkActivity {

    String foreignRecipient;
    Consumer<String> networkIDUpdate;
    public BootstrapActivity(BubblegumNode self, RouterNode to, String foreignRecipient, Consumer<String> networkIDUpdate) {
        super(self, to);
        this.foreignRecipient = foreignRecipient;
        this.networkIDUpdate = networkIDUpdate;
    }

    @Override
    public void run() {
        super.run();
        if(this.aborted) {
            this.onFail();
            return;
        }

        // Ping
        this.print("Starting bootstrapping process...  ("+this.to.getIPAddress().getHostAddress()+":"+this.to.getPort()+")");
        PingActivity ping = new PingActivity(this.localNode, this.to, this.foreignRecipient);
        ping.run();

        this.print("Bootstrap: Finished Initial Ping");

        if(ping.getSuccess()) {
            // Was a success, now bootstrapped. getNodes from bootstrapped node
            if(ping.getNetworkID() != null) {
                String oldNetworkID = this.localNode.getNetworkIdentifier();
                this.networkIDUpdate.accept(ping.getNetworkID());
                this.server.registerNewLocalNode(this.localNode, oldNetworkID);
            }
            else {
                this.print("Network ID from PING null");
            }

            RouterNode node = KademliaServerWorker.messageToRouterNode(ping.getPing());
            if(node != null && this.localNode.sync(node)) {
                this.print("Starting lookup");
                LookupActivity lookupActivity = new LookupActivity(this.localNode, this.localNode.getNodeIdentifier(), 10, false);
                lookupActivity.run();
                if (lookupActivity.getComplete() && lookupActivity.getSuccess()) this.onSuccess();
                else this.onFail();
            } else {
                this.print("Failed sync");
                this.onFail();
            }
        }

        else {
            this.onFail("No response from bootstrap node.");
        }
    }
}
