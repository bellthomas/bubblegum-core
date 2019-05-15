package io.hbt.bubblegum.core.kademlia.router;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.NodeID;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A representation of a peer's state.
 */
public class RouterNode implements Comparable<RouterNode> {
    private final NodeID node;
    private InetAddress ipAddress;
    private int port;
    private long latestResponse;
    private int failedResponses;

    /**
     * Constructor.
     * @param node The peer's identifier.
     * @param address The peer's IP address.
     * @param port the peer's port.
     */
    public RouterNode(NodeID node, InetAddress address, int port) {
        this.node = node;
        this.ipAddress = address;
        this.port = port;
        this.latestResponse = 0;
        this.failedResponses = 0;
    }

    /**
     * Build an instance from raw component data.
     * @param nodeID The peer's id.
     * @param address The peer's address.
     * @param port The peer's port.
     * @param latestResponse The peer's most recent response time.
     * @param failedResponses The peer's number of recent failed responses.
     * @return The RouterNode instance.
     */
    public static RouterNode buildFromSnapshotNode(String nodeID, String address, int port, long latestResponse, int failedResponses) {
        try {
            InetAddress ipAddress = NetworkingHelper.getInetAddress(address);
            NodeID id = new NodeID(nodeID);

            RouterNode node = new RouterNode(id, ipAddress, port);
            node.latestResponse = latestResponse;
            node.failedResponses = failedResponses;
            return node;

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (MalformedKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Mark that the peer has just successfully responded.
     */
    public void hasResponded() {
        this.failedResponses = 0;
        this.latestResponse = System.currentTimeMillis();
    }

    /**
     * Mark that the peer has just failed to respond to an RPC.
     */
    public void hasFailedToRespond() {
        this.failedResponses++;
    }

    /**
     * Retrieve the peer's identifier.
     * @return The peer's identifier.
     */
    public NodeID getNode() {
        return this.node;
    }

    /**
     * Retrieve the peer's latest response time.
     * @return The latest response time.
     */
    public long getLatestResponse() {
        return this.latestResponse;
    }

    /**
     * Retrieve the peer's number of recent failed responses.
     * @return The number of recent failed responses.
     */
    public int getFailedResponses() {
        return this.failedResponses;
    }

    /**
     * Retrieve the peer's port number.
     * @return The port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Retrieve a peer's IP address.
     * @return The address.
     */
    public InetAddress getIPAddress() {
        return ipAddress;
    }

    /**
     * Determine if a peer is "fresh"; whether it has been seen recently.
     * @return If the peer is fresh.
     */
    public boolean isFresh() {
        boolean override = (Configuration.random(100) >= Configuration.ROUTER_NODE_FRESH_OVERRIDE_PERCENTAGE);
        return (override && System.currentTimeMillis() - this.getLatestResponse() < Configuration.ROUTER_NODE_FRESH_EXPIRY);
    }

    /**
     * Generate the expected PGP UID of the peer.
     * @return
     */
    public String toPGPUID() {
        return String.join(";", this.ipAddress.getHostAddress(), this.port+"", this.node.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof RouterNode) {
            return this.node.equals(((RouterNode)obj).getNode());
        }
        return false;
    }

    @Override
    public int compareTo(RouterNode o) {
        if(this.equals(o)) return 0;
        return (this.getLatestResponse() > o.getLatestResponse()) ? 1 : -1;
    }

} // end RouterNode class
