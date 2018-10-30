package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.exceptions.BubblegumException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;

import java.util.HashSet;

public class BubblegumCell {

    public final static int CELL_SIZE = 25;
    private BubblegumCellServer server;
    private ActivityExecutionContext executionContext;
    private HashSet<BubblegumNode> nodes;

    public BubblegumCell(int port, ActivityExecutionContext executionContext) throws BubblegumException {
        this.server = new BubblegumCellServer(port, executionContext);
        this.executionContext = executionContext;
        this.nodes = new HashSet<>();
    }

    public synchronized BubblegumNode registerNode(BubblegumNode.Builder node) {
        if(this.nodes.size() < BubblegumCell.CELL_SIZE) {
            node.setServer(this.server);
            BubblegumNode finalNode = node.build();
            this.nodes.add(finalNode);
            return finalNode;
        }

        return null;
    }
}
