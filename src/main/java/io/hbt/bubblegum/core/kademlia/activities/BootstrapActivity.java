package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

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

        // Ping
        this.print("Starting bootstrapping process...  ("+this.to.getIPAddress().getHostAddress()+":"+this.to.getPort()+")");
        PingActivity ping = new PingActivity(this.localNode, this.to, this.foreignRecipient);
        ping.run();

        this.print("Bootstrap: Finished Initial Ping");

        if(ping.getSuccess()) {
            // Was a success, now bootstrapped. getNodes from bootstrapped node
            if(ping.getNetworkID() != null) {
                this.networkIDUpdate.accept(ping.getNetworkID());
                this.server.registerNewLocalNode(this.localNode);
            }
            else {
                this.print("Network ID from PING null");
            }
            // TODO delete old one

            this.print("Starting lookup");
            LookupActivity lookupActivity = new LookupActivity(this.localNode, this.localNode.getNodeIdentifier(), 10, false);
            lookupActivity.run();

            this.complete = true;
            this.success = (lookupActivity.getComplete() && lookupActivity.getSuccess());
        }

        else {
            this.print("No response from bootstrap node.");
        }
    }
}
