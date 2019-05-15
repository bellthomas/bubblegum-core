package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServerWorker;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.function.Consumer;


/**
 * Implements the Kademlia bootstrap operation.
 */
public class BootstrapActivity extends NetworkActivity {

    String foreignRecipient;
    Consumer<String> networkIDUpdate;

    /**
     * Constructor.
     * @param self The owning BubblegumNode.
     * @param to The node being bootstrapped onto.
     * @param foreignRecipient The foreignRecipient key of the peer.
     * @param networkIDUpdate A consumption function to update the local node's networkIdentifier.
     */
    public BootstrapActivity(BubblegumNode self, RouterNode to, String foreignRecipient, Consumer<String> networkIDUpdate) {
        super(self, to);
        this.foreignRecipient = foreignRecipient;
        this.networkIDUpdate = networkIDUpdate;
    }

    /**
     * Execute the RPC's logic.
     */
    @Override
    public void run() {
        super.run();
        if(this.aborted) {
            this.onFail();
            return;
        }

        // Ping
        PingActivity ping = new PingActivity(this.localNode, this.to, this.foreignRecipient);
        ping.run();


        if(ping.getSuccess()) {
            // Was a success, now bootstrapped. getNodes from bootstrapped node
            if(ping.getNetworkID() != null) {
                String oldNetworkID = this.localNode.getNetworkIdentifier();
                this.networkIDUpdate.accept(ping.getNetworkID());
                this.server.registerNewLocalNode(this.localNode, oldNetworkID);
            }

            RouterNode node = KademliaServerWorker.messageToRouterNode(ping.getPing());
            if(node != null && this.localNode.sync(node)) {
                LookupActivity lookupActivity = new LookupActivity(this.localNode, this.localNode.getNodeIdentifier(), 10, false);
                lookupActivity.run();
                if (lookupActivity.getComplete() && lookupActivity.getSuccess()) this.onSuccess();
                else this.onFail();
            } else {
                this.onFail();
            }
        }

        else {
            this.onFail("No response from bootstrap node.");
        }
    }

} // end BootstrapActivity class
