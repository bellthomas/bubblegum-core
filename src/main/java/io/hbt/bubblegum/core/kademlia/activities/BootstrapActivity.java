package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.function.Consumer;

public class BootstrapActivity extends NetworkActivity {

    String networkID;
    Consumer<String> networkIDUpdate;
    public BootstrapActivity(BubblegumNode self, RouterNode to, String networkID, Consumer<String> networkIDUpdate) {
        super(self, to);
        this.networkID = networkID;
        this.networkIDUpdate = networkIDUpdate;
    }


    @Override
    public void run() {
        // Ping
        this.print("Starting bootstrapping process...  ("+this.to.getIPAddress().getHostAddress()+":"+this.to.getPort()+")");
        PingActivity ping = new PingActivity(this.localNode, this.to, this.networkID);
        ping.run();

        this.print("Bootstrap: Finished Initial Ping");

        if(ping.getComplete()) {
            // Was a success, now bootstrapped. getNodes from bootstrapped node
            if(ping.getNetworkID() != null) this.networkIDUpdate.accept(ping.getNetworkID());

            LookupActivity lookupActivity = new LookupActivity(this.localNode, this.localNode.getNodeIdentifier(), 5, false);
            lookupActivity.run();

            this.complete = true;
            this.success = (lookupActivity.getComplete() && lookupActivity.getSuccess());
        }

        else {
            this.print("No response from bootstrap node.");
        }
    }
}
