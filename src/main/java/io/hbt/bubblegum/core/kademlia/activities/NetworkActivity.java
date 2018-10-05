package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServer;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;

import java.util.UUID;

public abstract class NetworkActivity implements Runnable {
    protected final KademliaServer server;
    protected final BubblegumNode localNode;
    protected final RouterNode to;
    protected final RoutingTable routingTable;
    protected String exchangeID;
    protected boolean isResponse;
    protected boolean complete;

    protected final static int TIMEOUT = 10; // seconds

    public NetworkActivity(KademliaServer server, BubblegumNode self, RouterNode to, RoutingTable routingTable) {
        this.server = server;
        this.localNode = self;
        this.to = to;
        this.routingTable = routingTable;
        this.exchangeID = UUID.randomUUID().toString();
        this.isResponse = false;
    }

    public void setResponse(String responseID) {
        this.isResponse = true;
        this.exchangeID = responseID;
    }


    public boolean getComplete() {
        return this.complete;
    }

    protected void timeoutOnComplete() {
        int i = 0;
        while(i < NetworkActivity.TIMEOUT && !this.complete) {
//            this.print("Wait Iteration " + i);
            try { Thread.sleep(1000); }
            catch (InterruptedException e) { e.printStackTrace(); }
            i++;
        }
    }

    protected void print(String msg) {
        System.out.println("["+this.server.getPort()+"] " + msg);
    }
}
