package io.hbt.bubblegum.core.kademlia.router;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.SortedSet;
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

    public void add(RouterNode node) {
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

                if(mostStale == null) {
                    this.replacements.add(node); // method?
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

//        for(RouterNode activeNode : this.activeBucket) {
//            if(activeNode.equals(node)) {
//                this.activeBucket.remove(activeNode);
//                return activeNode;
//            }
//        }
//        return null;
    }

    @Override
    public String toString() {
        return "Depth " + this.prefixLength + ": " + this.activeBucket.size() + " active, " + this.replacements.size() + " replacements";
    }
}
