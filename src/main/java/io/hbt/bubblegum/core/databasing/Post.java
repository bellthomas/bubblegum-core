package io.hbt.bubblegum.core.databasing;

import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem;


/**
 * Representation of a social network 'post'.
 */
public class Post {
    private final String id;
    private final String owner;
    private final String network;
    private String content;
    private String response;
    private final long timeCreated;

    /**
     * Constructor.
     * @param id The post's identifier.
     * @param owner The owner's identifier.
     * @param network The hosting network's identifier.
     * @param content The string content of the post.
     * @param response If applicable, the identifier of the post being responded to.
     * @param timeCreated The time the post was published (UNIX timestamp, seconds).
     */
    protected Post(String id, String owner, String network, String content, String response, long timeCreated) {
        this.id = id;
        this.owner = owner;
        this.network = network;
        this.content = content;
        this.response = response;
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

    public String getResponse() {
        return this.response;
    }

    public long getTimeCreated() {
        return this.timeCreated;
    }

    /**
     * Build a Post instance from a Protobuf response message.
     * @param responseItem The Protobuf message.
     * @return The Post instance.
     */
    public static Post fromKademliaQueryResponseItem(KademliaQueryResponseItem responseItem) {
        return new Post(
            responseItem.getId(),
            responseItem.getOwner(),
            responseItem.getNetwork(),
            responseItem.getContent(),
            responseItem.getResponse(),
            responseItem.getTime()
        );
    }

    @Override
    public String toString() {
        String val = "Origin: "+ this.network +":"+ this.owner +"\nPost: "+ this.owner +":"+ this.id +"\nTimestamp: " + this.timeCreated + "\nContent: " + this.content;
        if(this.response.length() > 0) val += "\n[In response to " + this.response + "]";
        return val;
    }
}
