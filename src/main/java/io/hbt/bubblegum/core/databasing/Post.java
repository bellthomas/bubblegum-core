package io.hbt.bubblegum.core.databasing;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.UUID;

public class Post {
    private final String id;
    private final String owner;
    private final String network;
    private String content;
    private final long timeCreated;

    public Post(BubblegumNode node, String content) {
        this.id = UUID.randomUUID().toString();
        this.owner = node.getNodeIdentifier().toString();
        this.network = node.getNetworkIdentifier();
        this.content = content;
        this.timeCreated = System.currentTimeMillis();
    }

    protected Post(String id, String owner, String network, String content, long timeCreated) {
        this.id = id;
        this.owner = owner;
        this.network = network;
        this.content = content;
        this.timeCreated = timeCreated;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getID() {
        return this.id;
    }

    public String getOwner() {
        return this.owner;
    }

    public String getNetwork() {
        return this.network;
    }

    public String getContent() {
        return this.content;
    }

    public long getTimeCreated() {
        return this.timeCreated;
    }

    public static Post fromKademliaQueryResponseItem(KademliaQueryResponseItem responseItem, BubblegumNode node) {
        return new Post(
            responseItem.getId(),
            responseItem.getOwner(),
            responseItem.getNetwork(),
            responseItem.getContent(),
            responseItem.getTime()
        );
    }

    @Override
    public String toString() {
        return "Owner: "+ this.network +":"+ this.owner +"\nPost #"+ this.id +"\nTimestamp: " + this.timeCreated + "\nContent: " + this.content;
    }
}
