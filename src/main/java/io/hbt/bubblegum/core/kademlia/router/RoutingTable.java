package io.hbt.bubblegum.core.kademlia.router;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.LookupActivity;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaNode.KademliaNode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * The representation of a BubblegumNode's routing table.
 */
public final class RoutingTable {

    protected final BubblegumNode self;
    private final RouterBucket[] buckets;

    /**
     * Constructor.
     * @param self The parent BubblegumNode.
     */
    public RoutingTable(BubblegumNode self) {
        this.self = self;
        this.buckets = new RouterBucket[Configuration.KEY_BIT_LENGTH];
    }

    /**
     * Insert a peer into the routing table.
     * @param node The peer to insert.
     */
    public void insert(RouterNode node) {
        this.getBucketForNode(node.getNode()).add(node);
    }

    /**
     * Get a bucket at a particular index.
     * @param index The bucket's index.
     * @return The bucket instance.
     */
    public RouterBucket getBucket(int index) {
        if(index < this.buckets.length && index >= 0) {
            if(this.buckets[index] == null) this.buckets[index] = new RouterBucket(index);
            return this.buckets[index];
        }
        return null;
    }

    /**
     * Calculate the correct bucket for a peer.
     * @param node The peer to find the bucket for.
     * @return The bucket instance.
     */
    public RouterBucket getBucketForNode(NodeID node) {
        int index = this.self.getNodeIdentifier().sharedPrefixLength(node) - 1;
        if(index < 0) return this.getBucket(0);
        else return this.getBucket(index);
    }

    /**
     * Find the closest nodes in the routing table to a key.
     * @param node The key to find nodes for.
     * @param nodesToGet The maximum number of nodes to return.
     * @return The set of closest peers.
     */
    public Set<RouterNode> getNodesClosestToKey(NodeID node, int nodesToGet) {
        return this.getNodesClosestToKeyWithExclusions(node, nodesToGet, new HashSet<>());
    }

    /**
     * Find the closest nodes in the routing table to a key.
     * @param node The key to find nodes for.
     * @param nodesToGet The maximum number of nodes to return.
     * @param exclusions Peers to exclude from the result.
     * @return The set of closest peers.
     */
    public Set<RouterNode> getNodesClosestToKeyWithExclusions(String node, int nodesToGet, Set<String> exclusions) {
        try {
            return this.getNodesClosestToKeyWithExclusions(new NodeID(node), nodesToGet, exclusions);
        } catch (MalformedKeyException e) {
            return new HashSet<>();
        }
    }

    /**
     * Find the closest nodes in the routing table to a key.
     * @param node The key to find nodes for.
     * @param nodesToGet The maximum number of nodes to return.
     * @param exclusions Peers to exclude from the result.
     * @return The set of closest peers.
     */
    public Set<RouterNode> getNodesClosestToKeyWithExclusions(NodeID node, int nodesToGet, Set<String> exclusions) {
        TreeSet<RouterNode> nodeDistanceTree = this.getAllNodesSorted(node.getKeyDistanceComparator());
        HashSet<RouterNode> results = new HashSet<>();

        Iterator<RouterNode> distanceTreeIterator = nodeDistanceTree.iterator();
        int i = 0;
        RouterNode n;
        while(distanceTreeIterator.hasNext() & i < nodesToGet) {
            n = distanceTreeIterator.next();
            if(!exclusions.contains(n.getNode().toString())) {
                results.add(n);
                i++;
            }
        }

        return results;
    }

    /**
     * Find to largest inhabited bucket.
     * @return The bucket's index.
     */
    public int getGreatestNonEmptyBucket() {
        int index = this.buckets.length - 1;
        while(this.buckets[index] == null && index > 0) index--;
        return index;
    }

    /**
     * Iterate over each bucket performing a lookup operation to refresh its state.
     */
    public void refreshBuckets() {
        int maximumNonEmptyBucket = this.self.getRoutingTable().getGreatestNonEmptyBucket() + 2;
        NodeID searchKey;
        for(int i = 0; i < maximumNonEmptyBucket; i++) {
            searchKey = this.self.getNodeIdentifier().generateIDWithSharedPrefixLength(i);
            Set<RouterNode> nodesToSearch = this.self.getRoutingTable().getNodesClosestToKey(searchKey, 3);
            for(RouterNode node : nodesToSearch) {
                LookupActivity lookupActivity1 = new LookupActivity(this.self, searchKey, 5, false);
                this.self.getExecutionContext().addActivity(this.self.getIdentifier(), lookupActivity1);
            }
        }
    }

    /**
     * Log the state of all buckets.
     */
    public void printBuckets() {
        for(RouterBucket bucket : this.buckets) {
            if(bucket != null) this.self.log(bucket.toString());
        }
    }

    /**
     * Sort all peers in the routing table.
     * @param comparator The sorting function.
     * @return The sorted peer set.
     */
    public TreeSet<RouterNode> getAllNodesSorted(Comparator<RouterNode> comparator) {
        TreeSet<RouterNode> nodeDistanceTree = new TreeSet<>(comparator);

        for(RouterBucket bucket : this.buckets) {
            if(bucket != null) nodeDistanceTree.addAll(bucket.getNodes());
        }

        return nodeDistanceTree;
    }

    /**
     * Find the RouterNode instance for a given NodeID.
     * @param id The id to search for.
     * @return The RouterNode object or null if not found.
     */
    public RouterNode getRouterNodeForID(NodeID id) {
        RouterBucket bucket = this.getBucketForNode(id);
        return bucket.getRouterNodeWithID(id);
    }

    /**
     * Get the total number of peers int he routing table.
     * @return The count.
     */
    public int getSize() {
        int i = 0;
        for(RouterBucket bucket : this.buckets) {
            if(bucket != null) i += bucket.getBucketSize();
        }
        return i;
    }

    /**
     * Build/fetch the RouterNode declared in a KademliaMessage.
     * @param node The message.
     * @return The RouterNode instance of the peer.
     */
    public RouterNode fromKademliaNode(KademliaNode node) {
        try {
            NodeID id = new NodeID(node.getHash());
            RouterNode result = this.getRouterNodeForID(id);
            if(result == null) {
                InetAddress address = NetworkingHelper.getInetAddress(node.getIpAddress());
                result = new RouterNode(id, address, node.getPort());
            }
            return result;

        } catch (MalformedKeyException e) {
            return null;
        } catch (UnknownHostException e) {
            return null;
        }
    }

} // end RoutingTable class
