package io.hbt.bubblegum.core.kademlia.router;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;

public class RoutingTable {

    private final BubblegumNode self;
    private final RouterBucket[] buckets;

    public RoutingTable(BubblegumNode self) {
        this.self = self;
        this.buckets = new RouterBucket[NodeID.KEY_BIT_LENGTH];
        for(int i = 0; i < NodeID.KEY_BIT_LENGTH; i++) this.buckets[i] = new RouterBucket(i);
    }

    public void insert(RouterNode node) {
//        System.out.println("inserting into routiung table: " + node.toString());
        this.getBucketForNode(node.getNode().getIdentifier()).add(node);
    }

    public RouterBucket getBucketForNode(NodeID node) {
        int index = this.self.getIdentifier().sharedPrefixLength(node) - 1;
//        System.out.println("index: " + index);
        if(index < 0) return this.buckets[0];
        else return this.buckets[index];
    }

    public void printBuckets() {
        for(RouterBucket bucket : this.buckets) {
            System.out.println(bucket.toString());
        }
    }
}
