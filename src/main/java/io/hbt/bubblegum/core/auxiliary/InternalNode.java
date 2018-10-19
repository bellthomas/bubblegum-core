package io.hbt.bubblegum.core.auxiliary;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;

import java.util.UUID;

public class InternalNode {
    private BubblegumNode node;
    private String identifier;

    public InternalNode(BubblegumNode node, String identifier) {
        this.node = node;
        this.identifier = identifier;
    }

    public InternalNode(BubblegumNode node) {
        this.node = node;
        this.identifier = UUID.randomUUID().toString();
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public BubblegumNode getNode() {
        return this.node;
    }
}