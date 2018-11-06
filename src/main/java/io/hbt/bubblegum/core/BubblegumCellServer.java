package io.hbt.bubblegum.core;

import co.paralleluniverse.fibers.Suspendable;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import io.hbt.bubblegum.core.exceptions.BubblegumException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServerWorker;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.activities.ActivityExecutionContext;
import io.hbt.bubblegum.core.kademlia.activities.DiscoveryActivity;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BubblegumCellServer {

    private int port;
    private InetAddress localAddress;
    private DatagramSocket listeningSocket;
    private ActivityExecutionContext executionContext;

    private static final int DATAGRAM_BUFFER_SIZE = 64 * 1024; // 64KB

    private final ConcurrentHashMap<String, Consumer<KademliaMessage>> responses = new ConcurrentHashMap<>();
    private final HashMap<String, BubblegumNode> recipients = new HashMap<>();

    private boolean alive = false;
    private Thread listenerThread;
    private static DatagramSocket sendingSocket;

    private long packetsSent = 0;
    private long packetsReceived = 0;

    public BubblegumCellServer(int port, ActivityExecutionContext executionContext) throws BubblegumException {

        try {
            this.listeningSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            try {
                System.out.println("Having to change port...");
                this.listeningSocket = new DatagramSocket(0);
            } catch (SocketException e1) {
                throw new BubblegumException();
            }
        }

        this.port = this.listeningSocket.getLocalPort();
        this.executionContext = executionContext;

        try {
            if(this.sendingSocket == null) this.sendingSocket = new DatagramSocket();
            this.alive = true;
            this.listenerThread = new Thread(() -> this.listen());
            this.listenerThread.setDaemon(true);
            this.listenerThread.start();
            this.print("BubblegumServer started on port " + this.port);

        } catch (SocketException e) {
            e.printStackTrace();
            throw new BubblegumException();
        }

        try {
            this.localAddress = InetAddress.getLocalHost();
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(
            () -> this.print("Stats  ~  Sent: " + this.packetsSent + ",  Received: " + this.packetsReceived),
            5,
            5,
            TimeUnit.SECONDS
        );
    }

    public void registerNewLocalNode(BubblegumNode node) {
        this.recipients.put(node.getNetworkIdentifier() + ":" + node.getNodeIdentifier().toString(), node);
    }

    private void listen() {
        while(this.alive) {
            try {
                byte[] buffer = new byte[DATAGRAM_BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                this.listeningSocket.receive(packet);
                this.packetsReceived++;

                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

                this.executionContext.addCallbackActivity("server", () -> {
                    try {
                        KademliaMessage message = KademliaMessage.parseFrom(data);

                        if (message.hasDiscoveryRequest()) {
                            if (this.recipients.size() > 0) {
                                this.recipients.keySet();
                                RouterNode sender = new RouterNode(
                                    new NodeID(message.getOriginHash()),
                                    InetAddress.getByName(message.getOriginIP()),
                                    message.getOriginPort()
                                );

                                String firstLocalID = (String) (this.recipients.keySet().toArray())[0];
                                BubblegumNode firstLocal = this.recipients.get(firstLocalID);

                                if (firstLocal != null) {
                                    DiscoveryActivity discover = new DiscoveryActivity(firstLocal, sender);
                                    discover.setResponse(message.getExchangeID(), this.recipients.keySet(), message.getOriginNetwork() + ":" + message.getOriginHash());
                                    firstLocal.getExecutionContext().addCallbackActivity(
                                        "server",
                                        () -> discover.run()
                                    );
                                }
                            }
                        }

                        else if (this.recipients.containsKey(message.getRecipient())) {
                            // Pass to them
                            BubblegumNode localRecipient = this.recipients.get(message.getRecipient());

                            if (this.responses.containsKey(localRecipient.getIdentifier() + ":" + message.getExchangeID())) {
                                Consumer<KademliaMessage> callback = this.responses.remove(localRecipient.getIdentifier() + ":" + message.getExchangeID());
                                if (callback != null) {
                                    callback.accept(message);
                                }
                            } else {
                                // Create worker to handle
                                KademliaServerWorker.accept(localRecipient, message);
                            }
                        }

                        // else: drop
                        else {
                            System.out.println("Dropped message to " + message.getRecipient());
                        }

                    } catch (InvalidProtocolBufferException ipbe) {
//                        MessageLite ml = ipbe.getUnfinishedMessage();
////                        ipbe.printStackTrace();
                        System.out.println("Corrupted packet");
                    } catch (MalformedKeyException e) {
                        e.printStackTrace();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void sendDatagram(BubblegumNode localNode, RouterNode node, KademliaMessage payload, Consumer<KademliaMessage> callback) {
        synchronized (this.sendingSocket) {
            try {
                if (callback != null) {
//                    System.out.println(this.responses == null);
//                    System.out.println(localNode == null);
//                    System.out.println(localNode.getIdentifier() == null);
//                    System.out.println(payload == null);
//                    System.out.println(payload.getExchangeID() == null);
                    this.responses.put(localNode.getIdentifier() + ":" + payload.getExchangeID(), callback);
                }

                DatagramPacket packet = new DatagramPacket(payload.toByteArray(), payload.toByteArray().length, node.getIPAddress(), node.getPort());
                if (this.sendingSocket == null) {
                    if(!this.sendingSocket.isConnected() || this.sendingSocket.isClosed()) this.sendingSocket.close();
                    this.sendingSocket = new DatagramSocket();
                }
                this.sendingSocket.send(packet);
//                new DatagramSocket().send(packet);
                this.packetsSent++;

            } catch (SocketException e) {
                System.out.println("[Socket Failure] " + e.getMessage());
//                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("[Socket Failure] " + e.getMessage());
//                e.printStackTrace();
            }
        }
    }

    public void removeCallback(String exchangeID) {
        // TODO fix for new ID ^
        if(this.responses.containsKey(exchangeID)) this.responses.remove(exchangeID);
    }

    public InetAddress getLocal() {
        return this.localAddress;
    }

    public int getPort() {
        return this.port;
    }

    private void print(String msg) {
        //this.localNode.log(msg);
    }
}
