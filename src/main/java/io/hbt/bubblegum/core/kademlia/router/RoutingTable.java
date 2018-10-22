package io.hbt.bubblegum.core.kademlia.router;

import io.hbt.bubblegum.core.databasing.SnapshotDatabase;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.FindActivity;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaNode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public final class RoutingTable {

    protected final BubblegumNode self;
    protected final RouterBucket[] buckets;

    public RoutingTable(BubblegumNode self) {
        this.self = self;
        this.buckets = new RouterBucket[NodeID.KEY_BIT_LENGTH];
        for(int i = 0; i < NodeID.KEY_BIT_LENGTH; i++) this.buckets[i] = new RouterBucket(i);

        // Try restore from snapshot
        Map<Integer, List<Set<RouterNode>>> snapshot = SnapshotDatabase.buildRoutingTableNodesFromSnapshot(self.getIdentifier());
        if(snapshot != null) {
            for (int i = 0; i < NodeID.KEY_BIT_LENGTH; i++) {
                if (snapshot.containsKey(i)) {
                    this.buckets[i].loadInSnapshotNodes(snapshot.get(i));
                }
            }
        }
    }

    public void insert(RouterNode node) {
        this.getBucketForNode(node.getNode()).add(node);
    }

    public RouterBucket getBucket(int index) {
        if(index < this.buckets.length && index >= 0) return this.buckets[index];
        else return null;
    }

    public RouterBucket getBucketForNode(NodeID node) {
        int index = this.self.getNodeIdentifier().sharedPrefixLength(node) - 1;
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
            searchKey = this.self.getNodeIdentifier().generateIDWithSharedPrefixLength(i);
            Set<RouterNode> nodesToSearch = this.self.getRoutingTable().getNodesClosestToKey(searchKey, 5);
            for(RouterNode node : nodesToSearch) {
                FindActivity findActivity = new FindActivity(this.self, node, searchKey.toString(), false);
                this.self.getExecutionContext().addActivity(this.self.getNodeIdentifier().toString(), findActivity);
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

    public RouterNode getRouterNodeForID(String id) {
        try {
            NodeID nodeID = new NodeID(id);
            return this.getRouterNodeForID(nodeID);
        } catch (MalformedKeyException e) {
            return null;
        }

    }

    public RouterNode getRouterNodeForID(NodeID id) {
        RouterBucket bucket = this.getBucketForNode(id);
        return bucket.getRouterNodeWithID(id);
    }

    public RouterNode fromKademliaNode(BgKademliaNode.KademliaNode node) {
        try {
            NodeID id = new NodeID(node.getHash());
            RouterNode result = this.getRouterNodeForID(id);
            if(result == null) {
                InetAddress address = InetAddress.getByName(node.getIpAddress());
                result = new RouterNode(id, address, node.getPort());
            }
            return result;

        } catch (MalformedKeyException e) {
            return null;
        } catch (UnknownHostException e) {
            return null;
        }
    }

}
