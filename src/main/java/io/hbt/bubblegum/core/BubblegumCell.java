package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.exceptions.BubblegumException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;

import java.util.HashSet;

public class BubblegumCell {

    private static final int MAX_CELLS = 50;
    private static int cellSize = 1;
    private static int totalNodes = 0;

    private BubblegumCellServer server;
    private HashSet<BubblegumNode> nodes;

    public BubblegumCell(int port, ActivityExecutionContext executionContext) throws BubblegumException {
        this.server = new BubblegumCellServer(port, executionContext);
        this.nodes = new HashSet<>();
    }

    public synchronized BubblegumNode registerNode(BubblegumNode.Builder node) {
        if(this.nodes.size() < BubblegumCell.cellSize) return this.internalAddNode(node);
        else return null;
    }

    protected synchronized BubblegumNode forceNode(BubblegumNode.Builder node) {
        return this.internalAddNode(node);
    }

    private BubblegumNode internalAddNode(BubblegumNode.Builder node) {
        node.setServer(this.server);
        BubblegumNode finalNode = node.build();
        this.nodes.add(finalNode);
        totalNodes++;
        cellSize = Math.floorDiv(MAX_CELLS + totalNodes, MAX_CELLS);
        return finalNode;
    }

    public int getPort() {
        return this.server.getPort();
    }
}
