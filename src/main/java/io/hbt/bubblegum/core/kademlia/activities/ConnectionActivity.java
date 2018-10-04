package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServer;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaConnectionMessage.KademliaConnectionMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;

import java.net.InetAddress;
import java.util.UUID;

public class ConnectionActivity implements Runnable {

    private final KademliaServer server;
    private final BubblegumNode from;
    private final RouterNode to;
    private final RoutingTable routingTable;
    private boolean complete = false;
    private boolean isResponse = false;
    private String exchangeID;

    private final static int TIMEOUT = 10; // seconds

    public ConnectionActivity(KademliaServer server, RouterNode connectTo, BubblegumNode from, RoutingTable routingTable) {
        this.server = server;
        this.from = from;
        this.to = connectTo;
        this.routingTable = routingTable;
        this.exchangeID = UUID.randomUUID().toString();
    }

    public void setResponse(String responseID) {
        this.isResponse = true;
        this.exchangeID = responseID;
    }

    @Override
    public void run() {
        KademliaMessage.Builder message = KademliaMessage.newBuilder();

        KademliaConnectionMessage.Builder connectionMessage = KademliaConnectionMessage.newBuilder();
        connectionMessage.setOriginHash(this.from.getIdentifier().toString());
        connectionMessage.setOriginIP(this.server.getLocal().getHostAddress());
        connectionMessage.setOriginPort(this.server.getPort());

        message.setConnectionMessage(connectionMessage.build());
        message.setExchangeID(this.exchangeID);

        this.server.sendDatagram(this.to, message.build(), (kademliaMessage -> {
            this.print("Connected to " + this.to.getNode().toString());
            this.routingTable.insert(this.to);
            this.complete = true;

            // Send response ConnectionMessage if this itself isn't a reply
            if(!this.isResponse) {
                this.server.sendDatagram(this.to, message.build(), null);
            }
        }));

        int i = 0;
        while(i < ConnectionActivity.TIMEOUT && !this.complete) {
//            this.print("Wait Iteration " + i);
            try { Thread.sleep(1000); }
            catch (InterruptedException e) { e.printStackTrace(); }
            i++;
        }
    }

    public boolean getComplete() {
        return this.complete;
    }

    private void print(String msg) {
        System.out.println("["+this.server.getPort()+"] " + msg);
    }
}
