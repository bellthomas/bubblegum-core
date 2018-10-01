package io.hbt.bubblegum.core.kademlia.router;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;

public class RouterNode implements Comparable<RouterNode> {
    private final BubblegumNode node;
    private long latestResponse;
    private int failedResponses;

    public RouterNode(BubblegumNode node) {
        this.node = node;
        this.latestResponse = System.nanoTime(); // TODO nano?
        this.failedResponses = 0;
    }

    public void hasResponded() {
        this.failedResponses = 0;
        this.latestResponse = System.nanoTime(); // TODO nano?
    }

    public void hasFailedToRespond() {
        this.failedResponses++;
    }

    public BubblegumNode getNode() {
        return this.node;
    }

    public long getLatestResponse() {
        return this.latestResponse;
    }

    public int getFailedResponses() {
        return this.failedResponses;
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
