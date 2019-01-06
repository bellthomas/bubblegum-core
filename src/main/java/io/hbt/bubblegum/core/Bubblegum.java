package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.databasing.Database;
import io.hbt.bubblegum.core.databasing.NetworkDetails;
import io.hbt.bubblegum.core.auxiliary.logging.LoggingManager;
import io.hbt.bubblegum.core.databasing.MasterDatabase;
import io.hbt.bubblegum.core.exceptions.AddressInitialisationException;
import io.hbt.bubblegum.core.exceptions.BubblegumException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;
import io.hbt.bubblegum.core.social.SocialIdentity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Bubblegum {

//    private InetAddress ipAddress;
    private SocialIdentity socialIdentity;
    private ActivityExecutionContext executionContext;
    private ArrayList<BubblegumCell> cells;
    private HashMap<String, BubblegumNode> nodes;

    private boolean isAlive = false;
    private boolean isShuttingDown = false;

    public Bubblegum(boolean reload) {

//        try {
//            this.initialiseIPAddress();
//            this.initialiseSocialIdentity();
            this.executionContext = new ActivityExecutionContext(1);
            this.cells = new ArrayList<>();
            this.nodes = new HashMap<>();

            if(reload) this.loadNodes();
            this.isAlive = true;

            Database.getInstance().initialiseExpiryScheduler(this.executionContext);

//        } catch (AddressInitialisationException e) {
//            System.out.println("Failed to start network");
//        }
//        catch (BubblegumException e) {
//            e.printStackTrace();
//        }
    }

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
                    reloadedNodeBuilder.setSocialIdentity(this.socialIdentity);
                    reloadedNodeBuilder.setExecutionContext(this.executionContext);
                    reloadedNodeBuilder.setLogger(LoggingManager.getLogger(network.getID()));
                    BubblegumNode reloadedNode = this.forceInsertIntoCell(network.getPort(), reloadedNodeBuilder);
                    reloadedNodeBuilder = null;

                    newProcesses++;
                    this.nodes.put(network.getID(), reloadedNode);
                } catch (MalformedKeyException e) {
                    System.out.println("Failed to load network - " + network.getHash());
                    continue;
                }
            }
        }

        this.executionContext.newProcessesInContext(newProcesses);
    }

//    private void initialiseIPAddress() throws AddressInitialisationException {
//        try {
//            this.ipAddress = InetAddress.getLocalHost();
//        } catch (UnknownHostException e) {
//            throw new AddressInitialisationException();
//        }
//    }


//    private void initialiseSocialIdentity() {
//        this.socialIdentity = new SocialIdentity();
//    }

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

    /* API */

    public Set<String> getNodeIdentifiers() {
        return this.nodes.keySet();
    }

    public BubblegumNode getNode(String identifier) {
        return this.nodes.get(identifier);
    }

    public void reset() {
        Database.getInstance().reset();
        this.nodes.clear();
    }

    public List<BubblegumNode> buildNodes(int numNodes) {
        List<BubblegumNode> nodes = new ArrayList<>();
        for(int i = 0; i < numNodes; i++) {
            nodes.add(this.createNode());
        }

//        Database.getInstance().updateNodesInDatabase(
//            this.nodes.entrySet().stream().map(
//                entry -> entry.getValue()
//            ).collect(Collectors.toList())
//        );
        return nodes;
    }


    public BubblegumNode createNode() {
        UUID identifier = UUID.randomUUID();
        this.executionContext.newProcessInContext();
        BubblegumNode.Builder newNodeBuilder = new BubblegumNode.Builder();
        newNodeBuilder.setIdentifier(identifier.toString());
        newNodeBuilder.setSocialIdentity(this.socialIdentity);
        newNodeBuilder.setExecutionContext(this.executionContext);
        newNodeBuilder.setLogger(LoggingManager.getLogger(identifier.toString()));
        BubblegumNode newNode = this.insertIntoCell(newNodeBuilder);
        newNodeBuilder = null;

        this.nodes.put(identifier.toString(), newNode);
        Database.getInstance().updateNodeInDatabase(newNode);
        return newNode;
    }

    public ActivityExecutionContext getExecutionContext() {
        return this.executionContext;
    }
}




/**
 *
 * https://stackoverflow.com/questions/19329682/adding-new-nodes-to-kademlia-building-kademlia-routing-tables
 *
 * http://gleamly.com/article/introduction-kademlia-dht-how-it-works
 *
 * PING probes a node to see if it’s online.
 *
 * STORE instructs a node to store a [key, value] pair for later retrieval
 *
 * FIND NODE takes a 160-bit key as an argument, the recipient of the FIND_NODE RPC returns information for the k nodes closest to the target id.
 *
 * FIND VALUE behaves like FIND_NODE returning the k nodes closest to the target Identifier with one exception – if the RPC recipient has received a STORE for the key, it just returns the stored value
 *
 */



