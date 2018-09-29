package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.social.SocialIdentity;

public class BubblegumNode {
    private SocialIdentity socialIdentity;
    private NodeID identifier;
    private int port;

    public BubblegumNode(SocialIdentity socialIdentity) {
        this.socialIdentity = socialIdentity;
        this.port = -1;
        this.identifier = new NodeID();
    }

    public BubblegumNode(SocialIdentity socialIdentity, int port) {
        this.socialIdentity = socialIdentity;
        this.port = port;
        this.identifier = new NodeID();
    }

    public BubblegumNode(SocialIdentity socialIdentity, String key, int port) {
        this.socialIdentity = socialIdentity;
        this.port = port;
        try {
            identifier = new NodeID(key);
        } catch (MalformedKeyException e) {
            identifier = new NodeID();
            System.out.println("Malformed Key (" + key + "), generated a new one (" + identifier.toString() + ")");
        }
    }

    @Override
    public String toString() {
        return identifier.toString();
    }
}
