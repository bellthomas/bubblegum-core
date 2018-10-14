package io.hbt.bubblegum.core.kademlia.router;

import io.hbt.bubblegum.core.kademlia.NodeID;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class RouterBucket {
    private final int prefixLength;
    private final ConcurrentSkipListSet<RouterNode> activeBucket;
    private final ConcurrentSkipListSet<RouterNode> replacements;

    /** Configuration **/
    private static final int BUCKET_SIZE = 8;

    public RouterBucket(int prefix) {
        this.prefixLength = prefix;
        this.activeBucket = new ConcurrentSkipListSet<>();
        this.replacements = new ConcurrentSkipListSet<>();
    }

    public synchronized void add(RouterNode node) {
        if(this.activeBucket.contains(node)) {
            RouterNode existingNode = this.removeFromActiveTable(node);
            existingNode.hasResponded();
            this.activeBucket.add(existingNode); // force resort
        }
        else {
            if(this.activeBucket.size() >= RouterBucket.BUCKET_SIZE) {
                // check if there is a stale node in the activeBucket to replace
                Comparator<RouterNode> failedResponsesComparator = Comparator.comparing(RouterNode::getFailedResponses);
                RouterNode mostStale = this.activeBucket.stream().max(failedResponsesComparator).get();

                if(mostStale == null || mostStale.getFailedResponses() == 0) {
                    this.addToReplacements(node);
                }
                else {
                    this.activeBucket.remove(mostStale);
                    this.activeBucket.add(node);
                }
            }
            else {
                this.activeBucket.add(node);
            }
        }
    }

    public RouterNode removeFromActiveTable(RouterNode node) {

        Optional<RouterNode> activeNode = this.activeBucket.stream().filter((n) -> n.equals(node)).findFirst();
        if(activeNode.isPresent()) {
            this.activeBucket.remove(activeNode.get());
            return activeNode.get();
        }
        else {
            return null;
        }
    }

    public void addToReplacements(RouterNode node) {
        if(this.replacements.contains(node)) {
            this.replacements.remove(node);
            node.hasResponded();
            this.replacements.add(node);
        }
        else if(this.replacements.size() >= RouterBucket.BUCKET_SIZE) {
            this.replacements.pollLast();
            this.replacements.add(node);
        }
        else {
            this.replacements.add(node);
        }
    }

    public Set<RouterNode> getNodes() {
        if(this.activeBucket.isEmpty()) return new HashSet<>();
        else return this.activeBucket.clone();
    }

    public RouterNode getRouterNodeWithID(NodeID id) {
        for(RouterNode node : this.activeBucket) if(node.getNode().equals(id)) return node;
        for(RouterNode node : this.replacements) if(node.getNode().equals(id)) return node;
        return null;
    }

    public int getBucketSize() {
        return this.activeBucket.size();
    }

    @Override
    public String toString() {
        return "Depth " + this.prefixLength + ": " + this.activeBucket.size() + " active, " + this.replacements.size() + " replacements";
    }
}
