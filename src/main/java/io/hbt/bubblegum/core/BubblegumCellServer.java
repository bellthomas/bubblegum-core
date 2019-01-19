package io.hbt.bubblegum.core;

import com.google.protobuf.InvalidProtocolBufferException;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.exceptions.BubblegumException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServerWorker;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;
import io.hbt.bubblegum.core.kademlia.activities.DiscoveryActivity;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.simulator.Metrics;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static io.hbt.bubblegum.core.Configuration.DATAGRAM_BUFFER_SIZE;

/**
 * Sending/Receiving Server attributed to a BubblegumCell.
 */
public class BubblegumCellServer {
    private boolean alive;
    private int port;
    private InetAddress localAddress;
    private DatagramSocket listeningSocket;
    private ActivityExecutionContext executionContext;
    private Thread listenerThread;
    private static DatagramSocket sendingSocket;

    private final ConcurrentHashMap<String, Consumer<KademliaMessage>> responses = new ConcurrentHashMap<>();
    private final HashMap<String, BubblegumNode> recipients = new HashMap<>();

    /**
     * Constructor.
     * @param port The port number to attempt to use.
     * @param executionContext The ActivityExecutionContext to use for async server operations.
     * @throws BubblegumException
     */
    public BubblegumCellServer(int port, ActivityExecutionContext executionContext) throws BubblegumException {

        // Initialise a socket to listen on.
        try {
            this.listeningSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            try {
                this.listeningSocket = new DatagramSocket(0);
                System.out.println("[BubblegumCellServer] Forced to change port from " + port + " to " + this.listeningSocket.getPort());
            } catch (SocketException e1) {
                throw new BubblegumException();
            }
        }

        this.port = this.listeningSocket.getLocalPort();
        this.executionContext = executionContext;

        // Initialise a socket to send on.
        try {
            if(this.sendingSocket == null) this.sendingSocket = new DatagramSocket();
            this.alive = true;
            this.listenerThread = new Thread(() -> this.listen());
            this.listenerThread.setDaemon(true);
            this.listenerThread.start();

        } catch (SocketException e) {
            e.printStackTrace();
            throw new BubblegumException();
        }

        this.localAddress = NetworkingHelper.getLocalInetAddress();
        if(this.localAddress == null) System.err.println("[BubblegumCellServer] Failed to initialise local address");
    }

    /**
     * Populate internal lookup table mapping recipient IDs to BubblegumNode instances.
     * @param node The BubblegumNode to be registered.
     */
    public void registerNewLocalNode(BubblegumNode node) {
        this.recipients.put(node.getNetworkIdentifier() + ":" + node.getNodeIdentifier().toString(), node);
    }

    /**
     * Repopulate internal node lookup table to update IDs after bootstrap operation.
     * @param node The BubblegumNode instance.
     * @param oldID The node's old ID.
     */
    public void registerNewLocalNode(BubblegumNode node, String oldID) {
        if(this.recipients.containsKey(oldID + ":" + node.getNodeIdentifier().toString())) {
            this.recipients.remove(oldID + ":" + node.getNodeIdentifier().toString());
        }
        this.registerNewLocalNode(node);
    }

    /**
     * Worker method for the receiving process.
     * Blocking, so needs to be spawned on a dedicated thread.
     */
    private void listen() {
        while(this.alive) {
            try {
                byte[] buffer = new byte[DATAGRAM_BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                this.listeningSocket.receive(packet);

                // Copy required as incorrect byte[] length will result in Protobuf failing to unpack the data.
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

                // Create async task for the bulk of the operation.
                this.executionContext.addCallbackActivity("system", () -> {

                    if(Metrics.isRecording()) Metrics.serverSubmission(data.length, true);
                    try {
                        KademliaMessage message = KademliaMessage.parseFrom(data);

                        // Detect Discovery process request (deprecated)
                        if (message.hasDiscoveryRequest()) {
                            if (this.recipients.size() > 0) {
                                this.recipients.keySet();
                                RouterNode sender = new RouterNode(
                                    new NodeID(message.getOriginHash()),
                                    NetworkingHelper.getInetAddress(message.getOriginIP()),
                                    message.getOriginPort()
                                );

                                String firstLocalID = (String) (this.recipients.keySet().toArray())[0];
                                BubblegumNode firstLocal = this.recipients.get(firstLocalID);

                                if (firstLocal != null) {
                                    DiscoveryActivity discover = new DiscoveryActivity(firstLocal, sender);
                                    discover.setResponse(message.getExchangeID(), this.recipients.keySet(), message.getOriginNetwork() + ":" + message.getOriginHash());
                                    firstLocal.getExecutionContext().addCallbackActivity(
                                        "system",
                                        () -> discover.run()
                                    );
                                }
                            }
                        }

                        // Pass the message to the node the message is addressed to or drop if recipient unknown.
                        else if (this.recipients.containsKey(message.getRecipient())) {
                            BubblegumNode localRecipient = this.recipients.get(message.getRecipient());
                            if (this.responses.containsKey(localRecipient.getIdentifier() + ":" + message.getExchangeID())) {
                                Consumer<KademliaMessage> callback = this.responses.remove(localRecipient.getIdentifier() + ":" + message.getExchangeID());
                                if (callback != null) {
                                    // We're expecting this packet, so pass to the method waiting on it.
                                    callback.accept(message);
                                }
                            } else {
                                // Invoke message handler.
                                KademliaServerWorker.accept(localRecipient, message);
                            }
                        }

                        // Unknown recipient, drop.
                        // else {
                        //     System.out.println("Dropped message to " + message.getRecipient());
                        // }

                    } catch (InvalidProtocolBufferException ipbe) {
                        // This is a corrupted packet.
                        // No action necessary, fault tolerance is inherent further up the stack.
                    } catch (MalformedKeyException e) {
                        // This is a corrupted packet.
                        // Invalid recipient information.
                    } catch (UnknownHostException e) {
                        // This is a corrupted packet.
                        // Invalid recipient address information.
                    }
                });

            } catch (IOException e) {
                // Network library failure on receiving a packet.
            }
        }
    }

    /**
     * General purpose DHT network sending function.
     * @param localNode The local node responsible for this transmission.
     * @param node The node instance the send the payload to.
     * @param payload The payload to be transmitted.
     * @param callback A callback function to be triggered when a response with the same exchange ID is received. May be null.
     */
    public void sendDatagram(BubblegumNode localNode, RouterNode node, KademliaMessage payload, Consumer<KademliaMessage> callback) {
        synchronized (this.sendingSocket) {
            try {
                if (callback != null) {
                    this.responses.put(localNode.getIdentifier() + ":" + payload.getExchangeID(), callback);
                }

                DatagramPacket packet = new DatagramPacket(payload.toByteArray(), payload.toByteArray().length, node.getIPAddress(), node.getPort());
                if (this.sendingSocket == null) {
                    if(!this.sendingSocket.isConnected() || this.sendingSocket.isClosed()) this.sendingSocket.close();
                    this.sendingSocket = new DatagramSocket();
                }
                this.sendingSocket.send(packet);

                if(Metrics.isRecording()) Metrics.serverSubmission(packet.getLength(), false);

            } catch (SocketException e) {
                // Network library exception.
                // Failure is handled further up the stack.
            } catch (IOException e) {
                // Network library exception.
                // Failure is handled further up the stack.
            }
        }
    }

    /**
     * Deregister a declared callback.
     * @param exchangeID The exchange ID to look for.
     */
    public void removeCallback(String exchangeID) {
        if(this.responses.containsKey(exchangeID)) this.responses.remove(exchangeID);
    }

    /**
     * Getter for this server's InetAddress instance.
     * @return The server's InetAddress instance.
     */
    public InetAddress getLocal() {
        return this.localAddress;
    }

    /**
     * Getter for the port this server is running on.
     * @return The server port.
     */
    public int getPort() {
        return this.port;
    }

} // end BubblegumCellServer class
