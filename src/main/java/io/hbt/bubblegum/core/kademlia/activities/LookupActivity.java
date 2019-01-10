package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.auxiliary.ComparableBytePayload;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class LookupActivity extends SystemActivity {

    private final static int alpha = 5;
    private final static int timeout = 3; // seconds
    private final NodeID nodeToLookup;
    private final int numResults;
    private final boolean getValue;

    private Set<ComparableBytePayload> results;
    private Set<RouterNode> closestNodes;
    private boolean foundFirstValue;
    private int opsSinceFirstFind;

    public LookupActivity(BubblegumNode localNode, NodeID lookup, int results, boolean getValue) {
        super(localNode);
        this.nodeToLookup = lookup;
        this.numResults = results;
        this.getValue = getValue;
        if(getValue) {
            this.results = new TreeSet<>();
            this.foundFirstValue = false;
            this.opsSinceFirstFind = 0;
        }
    }

    private enum FindStatus {
        IN_TRANSIT, COMPLETE
    }

    @Override
    public void run() {
        super.run();
        if(this.aborted) {
            this.onFail();
            return;
        }

        this.print("Running lookup...");

        // Check if we have a value locally first
        if(this.getValue && this.localNode.databaseHasKey(this.nodeToLookup.toString())) {
            this.results.addAll(
                ComparableBytePayload.fromCollection(this.localNode.databaseRetrieveValue(this.nodeToLookup.toString()))
            );
        }

        TreeSet<RouterNode> knownNodes = this.localNode.getRoutingTable().getAllNodesSorted(this.nodeToLookup.getKeyDistanceComparator());
        TreeSet<RouterNode> verifiedNodes = new TreeSet<>(this.nodeToLookup.getKeyDistanceComparator());

        TreeSet<RouterNode> shortlist = new TreeSet<>(this.nodeToLookup.getKeyDistanceComparator());
        shortlist.addAll(this.localNode.getRoutingTable().getNodesClosestToKey(this.nodeToLookup, alpha));

        HashMap<String, FindStatus> transitMatrix = new HashMap<>();

        RouterNode closestNode;
        RouterNode previousClosestNode = null;

        // At this point, shortlist contains the alpha closest nodes
        if(shortlist.isEmpty()) {
            this.onFail("No nodes to perform lookup via");
            return;
        }

        int opsWithoutNewClosest = 0;
        closestNode = shortlist.first();
        ArrayList<FindActivity> currentActivities = new ArrayList<>();

        long timeoutTime = System.currentTimeMillis() + timeout * 1000;
        while(opsWithoutNewClosest < alpha && System.currentTimeMillis() < timeoutTime) {
            Iterator<FindActivity> activityIterator = currentActivities.iterator();
            while (activityIterator.hasNext()) {
                FindActivity activity = activityIterator.next();

                if (activity == null) activityIterator.remove();
                else if (activity != null && activity.getComplete()) {
                    // Activity has completed
                    transitMatrix.put(activity.getDestination().getNode().toString(), FindStatus.COMPLETE);
                    opsWithoutNewClosest++;

                    if (activity.getSuccess()) {

                        verifiedNodes.add(activity.getDestination());

                        // If this is the new closest, save
                        if (this.nodeToLookup.getKeyDistanceComparator().compare(activity.getDestination(), closestNode) < 0) {
                            closestNode = activity.getDestination();
                            opsWithoutNewClosest = 0;
                            //sb.append("    ***** New closest - " + rNode.getNode().toString() + "\n");
                        }

                        if(this.getValue && this.foundFirstValue) {
                            this.opsSinceFirstFind++;
                        }

                        // FIND_VALUE check
                        List<byte[]> value = activity.getFindValueResult();
                        if (this.getValue && value != null) {
                            if(!this.foundFirstValue) this.foundFirstValue = true;
                            this.results.addAll(ComparableBytePayload.fromCollection(value));
                        }

                        // FIND_VALUE continuation for alpha ops
                        if(this.getValue && this.opsSinceFirstFind >= alpha) {
                            this.onSuccess("Retrieved " + this.results.size() + " values");
                            return;
                        }

                        // FIND_NODE check
                        Set<RouterNode> results = activity.getFindNodeResults();
                        if (results != null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(activity.getDestination().getNode() + " returned " + results.size() + " results;\n");
                            for (RouterNode rNode : results) {
                                // Exclude fresh nodes as they were already in the routing table and hence already in consideration
                                if (!transitMatrix.containsKey(rNode.getNode().toString()) && !rNode.isFresh()) {
                                    sb.append("--- " + rNode.getNode() + "\n");
                                    // Not yet seen/request in transit
                                    shortlist.add(rNode);
                                }
                                else {
                                    sb.append("--- " + rNode.getNode() + "  (Seen)\n");
                                }
                            }
                            this.print(sb.toString());
                        }
                    }

                    // Activity is complete, remove it
                    activityIterator.remove();
                }
            }


            if (currentActivities.size() < alpha) {

                // start new loop
                previousClosestNode = closestNode;
                if(currentActivities.isEmpty() && shortlist.isEmpty()) break;

                for (int i = 0; i < (alpha - currentActivities.size()); i++) {
                    if (!shortlist.isEmpty()) {
                        RouterNode toAdd = shortlist.pollFirst();
                        transitMatrix.put(toAdd.getNode().toString(), FindStatus.IN_TRANSIT);

                        this.print("FIND to " + toAdd.getNode().toString());
                        FindActivity find = new FindActivity(this.localNode, toAdd, this.nodeToLookup.toString(), this.getValue);
                        this.localNode.getExecutionContext().addActivity(this.localNode.getNodeIdentifier().toString(), find);
                        currentActivities.add(find);
                    }
                }

                this.print("Ops Without New Closest: " + opsWithoutNewClosest);

            } else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        this.closestNodes = new HashSet<>();
        for (int i = 0; i < this.numResults; i++) {
            if (!verifiedNodes.isEmpty()) this.closestNodes.add(verifiedNodes.pollFirst());
        }

        if(this.getValue && this.getClosestNodes().size() > 0) this.onSuccess();
        else if(this.closestNodes.size() > 0) this.onSuccess();
        else this.onFail();
    }

    public List<byte[]> getResult() {
        return this.results.stream().distinct().map((cbp) -> cbp.getPayload()).collect(Collectors.toList());
    }

    public Set<RouterNode> getClosestNodes() {
        return this.closestNodes;
    }
}
