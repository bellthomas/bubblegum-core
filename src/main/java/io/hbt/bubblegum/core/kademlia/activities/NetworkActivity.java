package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.BubblegumCellServer;
import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;

import java.util.UUID;


/**
 * Implementation of a generic UDP-based network activity.
 * Provides failure detection and automatic retransmission.
 */
public abstract class NetworkActivity extends SystemActivity {

    private int currentTry;

    protected final BubblegumCellServer server;
    protected final RouterNode to;
    protected final RoutingTable routingTable;
    protected String exchangeID;
    protected boolean isResponse;

    /**
     * Constructor.
     * @param self The owning BubblegumNode.
     * @param to The receiving peer.
     */
    public NetworkActivity(BubblegumNode self, RouterNode to) {
        super(self);
        this.server = self.getServer();
        this.to = to;
        this.routingTable = self.getRoutingTable();
        this.exchangeID = UUID.randomUUID().toString();
        this.isResponse = false;
        this.currentTry = 1;
    }

    /**
     * Run the activity's logic.
     */
    @Override
    public void run() {
        super.run();
    }

    /**
     * Print a log message.
     * @param msg The message.
     */
    @Override
    protected void print(String msg) {
        if(!isResponse) super.print(msg);
    }

    /**
     * Declare that this activity was created in response to another message.
     * @param responseID The original message's exchangeIdentifier.
     */
    public void setResponse(String responseID) {
        this.isResponse = true;
        this.exchangeID = responseID;
    }

    /**
     * Check if this activity is a response.
     * @return Response status.
     */
    public boolean isResponse() {
        return this.isResponse;
    }

    /**
     * Wait until the activity either times out or completes.
     * Retransmissions are automatically handled here.
     */
    @Override
    protected void timeoutOnComplete() {
        super.timeoutOnComplete();
        if(!this.complete) {
            if (this.currentTry < Configuration.NETWORK_ACTIVITY_RETRIES) {
                this.currentTry++;
                this.server.removeCallback(this.exchangeID);

                this.run();

            } else {
                // Timed-out
                RouterNode responder = this.routingTable.getRouterNodeForID(this.to.getNode());
                if (responder != null) responder.hasFailedToRespond();
                this.onFail("Timeout: RPC to " + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());
            }
        }
    }

} // end NetworkActivity class
