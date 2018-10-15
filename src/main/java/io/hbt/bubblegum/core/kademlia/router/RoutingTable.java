package io.hbt.bubblegum.core.kademlia.router;

import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.FindActivity;

import java.util.*;

public class RoutingTable {

    private final BubblegumNode self;
    private final RouterBucket[] buckets;

    public RoutingTable(BubblegumNode self) {
        this.self = self;
        this.buckets = new RouterBucket[NodeID.KEY_BIT_LENGTH];
        for(int i = 0; i < NodeID.KEY_BIT_LENGTH; i++) this.buckets[i] = new RouterBucket(i);
    }

    public void insert(RouterNode node) {
//        System.out.println("inserting into routing table: " + node.toString());
        this.getBucketForNode(node.getNode()).add(node);
    }

    public RouterBucket getBucketForNode(NodeID node) {
        int index = this.self.getIdentifier().sharedPrefixLength(node) - 1;
//        System.out.println("index: " + index);
        if(index < 0) return this.buckets[0];
        else return this.buckets[index];
    }

    public Set<RouterNode> getNodesClosestToKey(NodeID node, int nodesToGet) {
        return this.getNodesClosestToKeyWithExclusions(node, nodesToGet, new HashSet<>());
    }

    public Set<RouterNode> getNodesClosestToKeyWithExclusions(String node, int nodesToGet, Set<String> exclusions) {
        try {
            return this.getNodesClosestToKeyWithExclusions(new NodeID(node), nodesToGet, exclusions);
        } catch (MalformedKeyException e) {
            return new HashSet<>();
        }
    }

    public Set<RouterNode> getNodesClosestToKeyWithExclusions(NodeID node, int nodesToGet, Set<String> exclusions) {
        TreeSet<RouterNode> nodeDistanceTree = this.getAllNodesSorted(node.getKeyDistanceComparator());
        HashSet<RouterNode> results = new HashSet<>();

        Iterator<RouterNode> distanceTreeIterator = nodeDistanceTree.iterator();
        int i = 0;
        while(distanceTreeIterator.hasNext() & i < nodesToGet) {
            RouterNode n = distanceTreeIterator.next();
            // System.out.println(new BigInteger(1, node.xorDistance(n.getIdentifier())));
            if(!exclusions.contains(n.getNode().toString())) {
                results.add(n);
                i++;
            }
        }

        return results;
    }

    public int getGreatestNonEmptyBucket() {
        int index = this.buckets.length - 1;
        while(this.buckets[index].getBucketSize() == 0 && index > 0) index--;
        return index;
    }

    public void refreshBuckets() {
        this.self.log("Refreshing Buckets...");
        int maximumNonEmptyBucket = this.self.getRoutingTable().getGreatestNonEmptyBucket();
        NodeID searchKey;
        for(int i = 0; i < maximumNonEmptyBucket; i++) {
            searchKey = this.self.getIdentifier().generateIDWithSharedPrefixLength(i);
            Set<RouterNode> nodesToSearch = this.self.getRoutingTable().getNodesClosestToKey(searchKey, 5);
            for(RouterNode node : nodesToSearch) {
                FindActivity findActivity = new FindActivity(this.self.getServer(), this.self, node, this.self.getRoutingTable(), searchKey.toString());
                this.self.getExecutionContext().addActivity(this.self.getIdentifier().toString(), findActivity);
            }
        }
    }

    public void printBuckets() {
        for(RouterBucket bucket : this.buckets) {
            this.self.log(bucket.toString());
        }
    }

    public TreeSet<RouterNode> getAllNodesSorted(Comparator<RouterNode> comparator) {
        TreeSet<RouterNode> nodeDistanceTree = new TreeSet<>(comparator);

        for(RouterBucket bucket : this.buckets) {
            nodeDistanceTree.addAll(bucket.getNodes());
        }

        return nodeDistanceTree;
    }

    public RouterNode getRouterNodeForID(NodeID id) {
        RouterBucket bucket = this.getBucketForNode(id);
        return bucket.getRouterNodeWithID(id);
    }
}
