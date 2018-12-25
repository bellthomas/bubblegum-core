package io.hbt.bubblegum.core.kademlia.activities;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaNode.KademliaNode;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class LookupActivity extends SystemActivity {

    private final static int alpha = 5;
    private final NodeID nodeToLookup;
    private final int numResults;
    private final boolean getValue;

    private List<byte[]> results;
    private Set<RouterNode> closestNodes;

    public LookupActivity(BubblegumNode localNode, NodeID lookup, int results, boolean getValue) {
        super(localNode);
        this.nodeToLookup = lookup;
        this.numResults = results;
        this.getValue = getValue;
    }

//    @Override
//    public void run() {
//
//        // Check if we have a value locally first
//        if(this.getValue && this.localNode.databaseHasKey(this.nodeToLookup.toString())) {
//            this.result = this.localNode.databaseRetrieveValue(this.nodeToLookup.toString());
//            this.onSuccess("Lookup Value - found locally");
//            return;
//        }
//
//
//        RouterNode closestNode;
//        RouterNode previousClosestNode = null;
//        ArrayList<FindActivity> currentActivities = new ArrayList<>(alpha);
//
//        Set<RouterNode> contacted = new HashSet<>();
//        boolean finished = false;
//
//        // Need to generate K
//        TreeSet<RouterNode> knownNodes = this.localNode.getRoutingTable().getAllNodesSorted(this.nodeToLookup.getKeyDistanceComparator());
//        TreeSet<RouterNode> shortlist = new TreeSet<>(this.nodeToLookup.getKeyDistanceComparator());
//        for(int i = 0; i < alpha; i++) {
//            if(!knownNodes.isEmpty()) shortlist.add(knownNodes.pollFirst());
//        }
//
//        if(shortlist.isEmpty()) {
//            this.onFail("No nodes to search");
//            return;
//        }
//        closestNode = shortlist.first();
//
//        // shortlist currently empty
//        while(!finished) {
//
//            Iterator<FindActivity> activityItertator = currentActivities.iterator();
//            while(activityItertator.hasNext()) {
//                FindActivity findActivity = activityItertator.next();
//
//                if (findActivity != null & findActivity.getComplete()) {
//
//                    // Activity is finished, get results
//                    if (findActivity.getSuccess()) {
//                        // Complete and was successful
//                        contacted.add(findActivity.getDestination());
//
//                        byte[] value = findActivity.getFindValueResult();
//                        if(this.getValue && value != null) {
//                            // got our result
//                            this.result = value;
//                            this.onSuccess("Finished!\nValue: " + Arrays.toString(value));
//                            finished = true;
//                            return;
//                        }
//
//                        Set<KademliaNode> results = findActivity.getFindNodeResults();
//                        if (results != null) {
//                            StringBuilder sb = new StringBuilder();
//                            sb.append("\nFound Nodes:\n");
//                            for (KademliaNode kNode : results) {
//                                RouterNode rNode = this.localNode.getRoutingTable().fromKademliaNode(kNode);
//                                if (!contacted.contains(rNode) && !shortlist.contains(rNode) &&
//                                        !currentActivities.stream().anyMatch((n) -> n.getDestination().equals(rNode))) {
//                                    // Not already seen, not in shortlist and not currently being visited
//                                    sb.append("    --- " + rNode.getNode().toString() + "\n");
//                                    shortlist.add(rNode);
//                                    if(this.nodeToLookup.getKeyDistanceComparator().compare(rNode, closestNode) < 0) {
//                                        closestNode = rNode;
//                                        sb.append("    ***** New closest - " + rNode.getNode().toString() + "\n");
//                                    }
//                                } else {
//                                    sb.append("    Already seen - " + rNode.getNode().toString() + "\n");
//                                }
//                            }
//                            this.print(sb.toString());
//
//                        }
//                    }
//
//                    activityItertator.remove();
//                }
//            }
//
//            if(currentActivities.isEmpty()) {
//                if(closestNode == previousClosestNode) {
//                    // end of loop, break condition met
//
//                    TreeSet<RouterNode> finalClosestNodes = new TreeSet<>(this.nodeToLookup.getKeyDistanceComparator());
//                    finalClosestNodes.add(closestNode);
//                    finalClosestNodes.addAll(contacted);
//                    this.closestNodes = new HashSet<>();
//                    for(int i = 0; i < this.numResults; i++) {
//                        if(!finalClosestNodes.isEmpty()) this.closestNodes.add(finalClosestNodes.pollFirst());
//                    }
//
//                    this.onSuccess("Finished!\nClosest Node: " + closestNode.getNode().toString());
//                    finished = true;
//                }
//                else {
//                    // start new loop
//                    this.print("Starting new loop\n");
//                    previousClosestNode = closestNode;
//                    for(int i = 0; i < alpha; i++) {
//                        if(!shortlist.isEmpty()) {
//                            RouterNode toAdd = shortlist.pollFirst();
//                            if(this.nodeToLookup.getKeyDistanceComparator().compare(toAdd, closestNode) < 0) {
//                                closestNode = toAdd;
//                                this.print("***** New closest - " + toAdd.getNode().toString());
//                            }
//                            FindActivity find = new FindActivity(this.localNode, toAdd, this.nodeToLookup.toString(), this.getValue);
//                            this.localNode.getExecutionContext().addActivity(this.localNode.getNodeIdentifier().toString(), find);
//                            currentActivities.add(find);
//                        }
//                    }
//                }
//            } else {
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }


    private enum FindStatus {
        IN_TRANSIT, COMPLETE
    }

    @Override
    @Suspendable
    public void run() {
        this.print("Running lookup...");

        // Check if we have a value locally first
        if(this.getValue && this.localNode.databaseHasKey(this.nodeToLookup.toString())) {
            this.results = this.localNode.databaseRetrieveValue(this.nodeToLookup.toString());
            this.onSuccess("Lookup Value - found locally");
            return;
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

        while(opsWithoutNewClosest < 2 * alpha) {
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

                        // FIND_VALUE check
                        List<byte[]> value = activity.getFindValueResult();
                        if (this.getValue && value != null) {
                            // got our result
                            this.results = value;
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
                    Strand.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (SuspendExecution suspendExecution) {
                    suspendExecution.printStackTrace();
                }
            }
        }

        this.closestNodes = new HashSet<>();
        for (int i = 0; i < this.numResults; i++) {
            if (!verifiedNodes.isEmpty()) this.closestNodes.add(verifiedNodes.pollFirst());
        }
//
        this.onSuccess();
    }

    public List<byte[]> getResult() {
        return this.results;
    }

    public Set<RouterNode> getClosestNodes() {
        return this.closestNodes;
    }
}
