package io.hbt.bubblegum.core.kademlia.router;

import io.hbt.bubblegum.core.kademlia.NodeID;

import java.net.InetAddress;

/**
 * this is for other nodes, not us
 */
public class RouterNode implements Comparable<RouterNode> {
    private final NodeID node;
    private InetAddress ipAddress;
    private int port;
    private long latestResponse;
    private int failedResponses;

    public RouterNode(NodeID node, InetAddress address, int port) {
        this.node = node;
        this.ipAddress = address;
        this.port = port;
        this.latestResponse = 0;
        this.failedResponses = 0;
    }

    public void hasResponded() {
        this.failedResponses = 0;
        this.latestResponse = System.currentTimeMillis();
    }

    public void hasFailedToRespond() {
        this.failedResponses++;
    }

    public NodeID getNode() {
        return this.node;
    }

    public long getLatestResponse() {
        return this.latestResponse;
    }

    public int getFailedResponses() {
        return this.failedResponses;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getIPAddress() {
        return ipAddress;
    }

    public boolean isFresh() {
        return (System.currentTimeMillis() - this.getLatestResponse() < 60000);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof RouterNode) {
            return this.node.equals(((RouterNode)obj).getNode());
        }
        return false;
    }

    @Override
    public int compareTo(RouterNode o) {
        if(this.equals(o)) return 0;
        return (this.getLatestResponse() > o.getLatestResponse()) ? 1 : -1;
    }
}
