package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.exceptions.BubblegumException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;

import java.util.HashSet;

import static io.hbt.bubblegum.core.Configuration.MAX_BUBBLEGUM_CELLS;

/**
 * Internal class for managing the cell groupings of BubblegumNode instances.
 */
class BubblegumCell {

    private static int cellSize = 1;
    private static int totalNodes = 0;

    private BubblegumCellServer server;
    private HashSet<BubblegumNode> nodes;

    /**
     * Constructor.
     * @param port The port to attempt to bind this cell to.
     * @param executionContext The ActivityExecutionContext to bind the cell's server to.
     * @throws BubblegumException
     */
    public BubblegumCell(int port, ActivityExecutionContext executionContext) throws BubblegumException {
        this.server = new BubblegumCellServer(port, executionContext);
        this.nodes = new HashSet<>();
    }

    /**
     * Given a partial BubblegumNode.Builder instance, check if this instance can accommodate it
     * according to the scaling rules.
     * @param node The partially built node.
     * @return The build node if accepted or null if rejected.
     */
    public synchronized BubblegumNode registerNode(BubblegumNode.Builder node) {
        if(this.nodes.size() < BubblegumCell.cellSize) return this.internalAddNode(node);
        else return null;
    }

    /**
     * Experimental. Add a node regardless of whether the scaling rules would permit it or not.
     * @param node The partial node to build
     * @return The built node.
     */
    protected synchronized BubblegumNode forceNode(BubblegumNode.Builder node) {
        return this.internalAddNode(node);
    }

    /**
     * Internal method to register a node with the server and perform the final build.
     * @param node The partial node.
     * @return The built node.
     */
    private BubblegumNode internalAddNode(BubblegumNode.Builder node) {
        node.setServer(this.server);
        BubblegumNode finalNode = node.build();
        this.nodes.add(finalNode);
        this.totalNodes++;
        this.cellSize = Math.floorDiv(MAX_BUBBLEGUM_CELLS + totalNodes, MAX_BUBBLEGUM_CELLS);
        return finalNode;
    }

    /**
     * Get the port associated with this cell's server.
     * @return The port number.
     */
    public int getPort() {
        return this.server.getPort();
    }

} // end BubblegumCell class
