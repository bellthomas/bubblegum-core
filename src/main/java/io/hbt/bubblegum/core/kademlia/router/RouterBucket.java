package io.hbt.bubblegum.core.kademlia.router;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.kademlia.NodeID;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

public final class RouterBucket {
    protected final int prefixLength;
    protected ConcurrentSkipListSet<RouterNode> activeBucket;
    protected ConcurrentSkipListSet<RouterNode> replacements;

    protected int activeNodes, replacementNodes = 0;

    public RouterBucket(int prefix) {
        this.prefixLength = prefix;
//        this.activeBucket = new ConcurrentSkipListSet<>();
//        this.replacements = new ConcurrentSkipListSet<>();
    }

    public synchronized void add(RouterNode node) {
        if(this.activeBucket == null) this.activeBucket = new ConcurrentSkipListSet<>();
        if(this.activeBucket.stream().anyMatch((n) -> n.equals(node))) {
            RouterNode existingNode = this.removeFromActiveTable(node);
            if(existingNode != null) {
                existingNode.hasResponded();
                this.activeBucket.add(existingNode); // force a re-sort
                this.activeNodes++;
            }
        }
        else {
            node.hasResponded();

            if(this.activeNodes >= Configuration.ROUTER_BUCKET_SIZE) {
                // check if there is a stale node in the activeBucket to replace
                Comparator<RouterNode> failedResponsesComparator = Comparator.comparing(RouterNode::getFailedResponses);
                RouterNode mostStale = this.activeBucket.stream().max(failedResponsesComparator).get();

                if(mostStale == null || mostStale.getFailedResponses() == 0) {
                    this.addToReplacements(node);
                }
                else {
                    this.activeBucket.remove(mostStale);
                    if(this.replacementNodes < Configuration.ROUTER_BUCKET_SIZE) {
                        if(this.replacements == null) this.replacements = new ConcurrentSkipListSet<>();
                        this.replacements.add(mostStale);
                        this.replacementNodes++;
                    }
                    this.activeBucket.add(node);
                }
            }
            else {
                this.activeBucket.add(node);
                this.activeNodes++;
            }
        }
    }

    protected synchronized RouterNode removeFromActiveTable(RouterNode node) {
        if(this.activeBucket == null) return null;
        Optional<RouterNode> activeNode = this.activeBucket.stream().filter((n) -> n.equals(node)).findFirst();
        if(activeNode.isPresent()) {
            this.activeBucket.remove(activeNode.get());
            this.activeNodes--;
            return activeNode.get();
        }
        else {
            System.out.println("returned null");
            return null;
        }
    }

    protected synchronized void addToReplacements(RouterNode node) {
        // TODO investigate why this works and contains() doesn't
        if(this.replacements == null) this.replacements = new ConcurrentSkipListSet<>();
        if(this.replacements.stream().anyMatch((n) -> n.equals(node))) {
            this.replacements.remove(node);
            node.hasResponded();
            this.replacements.add(node);
        }
        else if(this.replacementNodes >= Configuration.ROUTER_BUCKET_SIZE) {
            this.replacements.pollLast();
            node.hasResponded();
            this.replacements.add(node);
        }
        else {
            node.hasResponded();
            this.replacements.add(node);
            this.replacementNodes++;
        }
    }

    public Set<RouterNode> getNodes() {
        if(this.activeBucket == null || this.activeBucket.isEmpty()) return new HashSet<>();
        else {
            HashSet<RouterNode> result = new HashSet<>();
            result.addAll(this.activeBucket);
            if(this.replacements != null) result.addAll(this.replacements);
            return result;
        }
    }

    public Set<RouterNode> getReplacementNodes() {
        if(this.replacements == null || this.replacements.isEmpty()) return new HashSet<>();
        else {
            HashSet<RouterNode> result = new HashSet<>();
            result.addAll(this.replacements);
            return result;
        }
    }

    public RouterNode getRouterNodeWithID(NodeID id) {
        if(this.activeBucket != null)
            for(RouterNode node : this.activeBucket) if(node.getNode().equals(id)) return node;
        if(this.replacements != null)
            for(RouterNode node : this.replacements) if(node.getNode().equals(id)) return node;
        return null;
    }

    public int getBucketSize() {
        return this.activeNodes + this.replacementNodes;
    }

    public int getPrefixLength() {
        return this.prefixLength;
    }

    protected synchronized void loadInSnapshotNodes(List<Set<RouterNode>> nodes) {
        Set<RouterNode> active = nodes.get(0);
        if(active != null) {
            for(RouterNode node : active) {
                if(this.activeNodes < Configuration.ROUTER_BUCKET_SIZE) {
                    if(this.activeBucket == null) this.activeBucket = new ConcurrentSkipListSet<>();
                    this.activeBucket.add(node);
                    this.activeNodes++;
                }
            }
        }

        Set<RouterNode> replacements = nodes.get(1);
        if(replacements != null) {
            for(RouterNode node : replacements) {
                if(this.replacementNodes < Configuration.ROUTER_BUCKET_SIZE) {
                    if(this.replacements == null) this.replacements = new ConcurrentSkipListSet<>();
                    this.replacements.add(node);
                    this.replacementNodes++;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Depth " + this.prefixLength + ": " + this.activeNodes + " active, " + this.replacementNodes + " replacements";
    }
}
