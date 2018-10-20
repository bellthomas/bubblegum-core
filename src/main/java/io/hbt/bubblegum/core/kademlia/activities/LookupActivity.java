package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.auxiliary.logging.Logger;
import io.hbt.bubblegum.core.auxiliary.logging.LoggingManager;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaNode.KademliaNode;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.*;

public class LookupActivity extends SystemActivity {

    private final static int alpha = 5;
    private final NodeID nodeToLookup;
    private final int numResults;
    private final boolean getValue;

    private byte[] result;
    private Set<RouterNode> closestNodes;

    public LookupActivity(BubblegumNode localNode, NodeID lookup, int results, boolean getValue) {
        super(localNode);
        this.nodeToLookup = lookup;
        this.numResults = results;
        this.getValue = getValue;
    }

    @Override
    public void run() {
        RouterNode closestNode;
        RouterNode previousClosestNode = null;
        ArrayList<FindActivity> currentActivities = new ArrayList<>(alpha);

        Set<RouterNode> contacted = new HashSet<>();
        boolean finished = false;

        // Need to generate K
        TreeSet<RouterNode> knownNodes = this.localNode.getRoutingTable().getAllNodesSorted(this.nodeToLookup.getKeyDistanceComparator());
        TreeSet<RouterNode> shortlist = new TreeSet<>(this.nodeToLookup.getKeyDistanceComparator());
        for(int i = 0; i < alpha; i++) {
            if(!knownNodes.isEmpty()) shortlist.add(knownNodes.pollFirst());
        }

        if(shortlist.isEmpty()) {
            this.onFail("No nodes to search");
            return;
        }
        closestNode = shortlist.first();

        // shortlist currently empty
        while(!finished) {

            Iterator<FindActivity> activityItertator = currentActivities.iterator();
            while(activityItertator.hasNext()) {
                FindActivity findActivity = activityItertator.next();

                if (findActivity != null & findActivity.getComplete()) {

                    // Activity is finished, get results
                    if (findActivity.getSuccess()) {
                        // Complete and was successful
                        contacted.add(findActivity.getDestination());

                        byte[] value = findActivity.getFindValueResult();
                        if(this.getValue && value != null) {
                            // got our result
                            this.result = value;
                            this.onSuccess("Finished!\nValue: " + Arrays.toString(value));
                            finished = true;
                            return;
                        }

                        Set<KademliaNode> results = findActivity.getFindNodeResults();
                        if (results != null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("\nFound Nodes:\n");
                            for (KademliaNode kNode : results) {
                                RouterNode rNode = this.localNode.getRoutingTable().fromKademliaNode(kNode);
                                if (!contacted.contains(rNode) && !shortlist.contains(rNode) &&
                                        !currentActivities.stream().anyMatch((n) -> n.getDestination().equals(rNode))) {
                                    // Not already seen, not in shortlist and not currently being visited
                                    sb.append("    --- " + rNode.getNode().toString() + "\n");
                                    shortlist.add(rNode);
                                    if(this.nodeToLookup.getKeyDistanceComparator().compare(rNode, closestNode) < 0) {
                                        closestNode = rNode;
                                        sb.append("    ***** New closest - " + rNode.getNode().toString() + "\n");
                                    }
                                } else {
                                    sb.append("    Already seen - " + rNode.getNode().toString() + "\n");
                                }
                            }
                            this.print(sb.toString());

                        }
                    }

                    activityItertator.remove();
                }
            }

            if(currentActivities.isEmpty()) {
                if(closestNode == previousClosestNode) {
                    // end of loop, break condition met

                    TreeSet<RouterNode> finalClosestNodes = new TreeSet<>(this.nodeToLookup.getKeyDistanceComparator());
                    finalClosestNodes.add(closestNode);
                    finalClosestNodes.addAll(contacted);
                    this.closestNodes = new HashSet<>();
                    for(int i = 0; i < this.numResults; i++) {
                        if(!finalClosestNodes.isEmpty()) this.closestNodes.add(finalClosestNodes.pollFirst());
                    }

                    this.onSuccess("Finished!\nClosest Node: " + closestNode.getNode().toString());
                    finished = true;
                }
                else {
                    // start new loop
                    this.print("Starting new loop\n");
                    previousClosestNode = closestNode;
                    for(int i = 0; i < alpha; i++) {
                        if(!shortlist.isEmpty()) {
                            RouterNode toAdd = shortlist.pollFirst();
                            if(this.nodeToLookup.getKeyDistanceComparator().compare(toAdd, closestNode) < 0) {
                                closestNode = toAdd;
                                this.print("***** New closest - " + toAdd.getNode().toString());
                            }
                            FindActivity find = new FindActivity(this.localNode, toAdd, this.nodeToLookup.toString(), this.getValue);
                            this.localNode.getExecutionContext().addActivity(this.localNode.getNodeIdentifier().toString(), find);
                            currentActivities.add(find);
                        }
                    }
                }
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public byte[] getResult() {
        return this.result;
    }

    public Set<RouterNode> getClosestNodes() {
        return this.closestNodes;
    }
}
