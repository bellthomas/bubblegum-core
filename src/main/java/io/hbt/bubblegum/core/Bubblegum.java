package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.databasing.Database;
import io.hbt.bubblegum.core.databasing.NetworkDetails;
import io.hbt.bubblegum.core.exceptions.BubblegumException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The core Bubblegum management class.
 * @author Harri Bell-Thomas, ahb36@cam.ac.uk
 */
public class Bubblegum {
    private ActivityExecutionContext executionContext;
    private ArrayList<BubblegumCell> cells;
    private HashMap<String, BubblegumNode> nodes;

    /**
     * Constructor.
     * @param reload Whether to attempt to reload the previous saved session from disk (experimental).
     */
    public Bubblegum(boolean reload) {
        this.executionContext = new ActivityExecutionContext(1);
        this.cells = new ArrayList<>();
        this.nodes = new HashMap<>();

        if(reload) this.loadNodes();
        Database.getInstance().initialiseExpiryScheduler(this.executionContext);
    }


    //region API

    /**
     * Generate and initialise a new local BubblegumNode.
     * @return The BubblegumNode instance.
     */
    public BubblegumNode createNode() {
        this.executionContext.newProcessInContext();
        BubblegumNode.Builder newNodeBuilder = new BubblegumNode.Builder();
        newNodeBuilder.setExecutionContext(this.executionContext);
        BubblegumNode newNode = this.insertIntoCell(newNodeBuilder);
        newNodeBuilder = null;

        this.nodes.put(newNode.getIdentifier(), newNode);
        // Database.getInstance().updateNodeInDatabase(newNode);
        return newNode;
    }


    /**
     * Generate and initialise multiple new local BubblegumNodes.
     * @param numNodes The number of nodes to create.
     * @return The BubblegumNode instances created.
     */
    public List<BubblegumNode> buildNodes(int numNodes) {
        List<BubblegumNode> nodes = new ArrayList<>();
        for(int i = 0; i < numNodes; i++) {
            nodes.add(this.createNode());
        }

        /* Database.getInstance().updateNodesInDatabase(
            this.nodes.entrySet().stream().map(
                entry -> entry.getValue()
            ).collect(Collectors.toList())
        ); */

        return nodes;
    }


    /**
     * Retrieve all the identifiers of the nodes managed by this Bubblegum instance.
     * @return The BubblegumNode identifiers.
     */
    public Set<String> getNodeIdentifiers() {
        return this.nodes.keySet();
    }


    /**
     * Get the BubblegumNode instance associated with an ID.
     * @param identifier The ID to resolve.
     * @return The BubblegumNode instance or null for an invalid ID.
     */
    public BubblegumNode getNode(String identifier) {
        return this.nodes.get(identifier);
    }


    /**
     * Reset the instance, deleting all nodes.
     */
    public void reset() {
        Database.getInstance().reset();
        this.nodes.clear();
    }


    /**
     * Retrieve the ActivityExecutionContext shared by all entities in this Bubblegum instance.
     * @return The ActivityExecutionContext instance.
     */
    public ActivityExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    //endregion

    //region Internal

    /**
     * Internal management method.
     * Given a partially build BubblegumNode instance, register and build it within a system BubblegumCell instance.
     * @param node The partially built BubblegumNode.Builder node.
     * @return The fully built and registered BubblegumNode.
     */
    private BubblegumNode insertIntoCell(BubblegumNode.Builder node) {
        BubblegumNode result = null;
        int i = 0;
        while(result == null && i < this.cells.size()) {
            result = this.cells.get(i).registerNode(node);
            i++;
        }

        if(result == null) {
            // Need to create new cell
            try {
                BubblegumCell newCell = new BubblegumCell(0, this.executionContext);
                this.cells.add(newCell);
                result = newCell.registerNode(node);

            } catch (BubblegumException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    //endregion

    /** EXPERIMENTAL **/

    private void loadNodes() {
        // TODO assert nodes and cells empty
        Map<Integer, List<NetworkDetails>> networks = Database.getInstance().loadNetworksFromDatabase();
        int newProcesses = 0;

        for(Map.Entry<Integer, List<NetworkDetails>> networkEntry : networks.entrySet()) {
            for(NetworkDetails network : networkEntry.getValue()) {
                if (this.nodes.containsKey(network.getID())) continue;
                try {
                    NodeID id = new NodeID(network.getHash());

                    BubblegumNode.Builder reloadedNodeBuilder = new BubblegumNode.Builder();
                    reloadedNodeBuilder.setIdentifier(network.getID());
                    reloadedNodeBuilder.setNetworkIdentifier(network.getNetwork());
                    reloadedNodeBuilder.setNodeIdentifier(id);
                    reloadedNodeBuilder.setPort(network.getPort());
                    reloadedNodeBuilder.setExecutionContext(this.executionContext);
                    BubblegumNode reloadedNode = this.forceInsertIntoCell(network.getPort(), reloadedNodeBuilder);
                    reloadedNodeBuilder = null;

                    newProcesses++;
                    this.nodes.put(reloadedNode.getIdentifier(), reloadedNode);
                } catch (MalformedKeyException e) {
                    System.out.println("Failed to load network - " + network.getHash());
                    continue;
                }
            }
        }

        this.executionContext.newProcessesInContext(newProcesses);
    }


    private BubblegumNode forceInsertIntoCell(int port, BubblegumNode.Builder node) {

        BubblegumCell chosenCell = null;
        for(BubblegumCell cell : this.cells) {
            if(cell.getPort() == port) {
                chosenCell = cell;
                break;
            }
        }

        BubblegumNode result = null;
        if(chosenCell == null) {
            try {
                BubblegumCell newCell = new BubblegumCell(port, this.executionContext);
                this.cells.add(newCell);
                result = newCell.forceNode(node);
            } catch (BubblegumException e) {
                e.printStackTrace();
            }
        }
        else {
            result = chosenCell.forceNode(node);
        }

        return result;
    }

    //endregion

} // end Bubblegum class