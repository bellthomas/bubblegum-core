package io.hbt.bubblegum.core.kademlia.activities;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import io.hbt.bubblegum.core.BubblegumCellServer;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;

import java.util.UUID;

public abstract class NetworkActivity extends SystemActivity {

    private final static int RETRIES = 3;

    private int currentTry;

    protected final BubblegumCellServer server;
    protected final RouterNode to;
    protected final RoutingTable routingTable;
    protected String exchangeID;
    protected boolean isResponse;

    public NetworkActivity(BubblegumNode self, RouterNode to) {
        super(self);
        this.server = self.getServer();
        this.to = to;
        this.routingTable = self.getRoutingTable();
        this.exchangeID = UUID.randomUUID().toString();
        this.isResponse = false;
        this.currentTry = 1;
    }

    @Override
    protected void print(String msg) {
        if(!isResponse) super.print(msg);
    }

    public void setResponse(String responseID) {
        this.isResponse = true;
        this.exchangeID = responseID;
    }

    @Override
    @Suspendable
    protected void timeoutOnComplete() {
        super.timeoutOnComplete();
        if(!this.complete) {
            if (this.currentTry < RETRIES) {
                this.currentTry++;
                this.server.removeCallback(this.exchangeID);
                try {
                    this.run();
                } catch (SuspendExecution suspendExecution) {
                    RouterNode responder = this.routingTable.getRouterNodeForID(this.to.getNode());
                    if (responder != null) responder.hasFailedToRespond();
                    this.onFail("Failed: RPC to " + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());
                } catch (InterruptedException e) {
                    RouterNode responder = this.routingTable.getRouterNodeForID(this.to.getNode());
                    if (responder != null) responder.hasFailedToRespond();
                    this.onFail("Failed: RPC to " + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());                }
            } else {
                // Timed-out
                RouterNode responder = this.routingTable.getRouterNodeForID(this.to.getNode());
                if (responder != null) responder.hasFailedToRespond();
                this.onFail("Timeout: RPC to " + this.to.getIPAddress().getHostAddress() + ":" + this.to.getPort());
            }
        }
    }
}
