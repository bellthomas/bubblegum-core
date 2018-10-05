package io.hbt.bubblegum.core.kademlia;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import io.hbt.bubblegum.core.auxiliary.ConcurrentBlockingQueue;
import io.hbt.bubblegum.core.exceptions.BubblegumException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.activities.FindNodeActivity;
import io.hbt.bubblegum.core.kademlia.activities.PingActivity;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaPing.KademliaPing;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class KademliaServer {
    private final BubblegumNode localNode;
    private final int port;
    private InetAddress localAddress;
    private final DatagramSocket listeningSocket;
    private final ConcurrentBlockingQueue<DatagramPacket> kademliaHandlers;
    private static final int DATAGRAM_BUFFER_SIZE = 64 * 1024;      // 64KB

    ConcurrentHashMap<String, Consumer<KademliaMessage>> responses = new ConcurrentHashMap<>();

    private boolean alive = false;
    private Thread listenerThread;

    public KademliaServer(BubblegumNode local, int port) throws BubblegumException {
        this.localNode = local;
        this.port = port;
        this.kademliaHandlers = new ConcurrentBlockingQueue<>();
        try {
            this.listeningSocket = new DatagramSocket(this.port);
            this.alive = true;
            this.listenerThread = new Thread(() -> this.listen());
            this.listenerThread.setDaemon(true);
            this.listenerThread.start();
            this.print("KademliaServer started on port " + this.port);

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
    }

    private void listen() {
        while(this.alive) {
            try {
                byte[] buffer = new byte[DATAGRAM_BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                this.listeningSocket.receive(packet);

                this.print("Received");

                // Resize the array to avoid padding issues
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

//                this.kademliaHandlers.put(packet);

                try {
                    KademliaMessage message = KademliaMessage.parseFrom(data);

                    if(this.responses.containsKey(message.getExchangeID())) {
                        this.print("Callback found for " + message.getExchangeID());
                        Consumer<KademliaMessage> callback = this.responses.remove(message.getExchangeID());
                        if(callback != null) new Thread(() -> callback.accept(message)).start();
                    }

                    else if(message.hasPingMessage()) {
                        this.print("Ping Message Received");

                        try {
                            RouterNode sender = new RouterNode(
                                    new NodeID(message.getOriginHash()),
                                    InetAddress.getByName(message.getOriginIP()),
                                    message.getOriginPort()
                            );
                            this.localNode.getRoutingTable().insert(sender);
                            this.print("Connection Message Sender: " + sender.getNode().toString());

                            PingActivity pingReply = new PingActivity(this, this.localNode, sender, this.localNode.getRoutingTable());
                            pingReply.setResponse(message.getExchangeID());
                            new Thread(() -> pingReply.run()).start();

                        } catch (MalformedKeyException e) {
                            e.printStackTrace();
                        }
                    }

                    else if(message.hasFindNodeRequest()) {
                        this.print("FindNode Request Received: " + message.getFindNodeRequest().getSearchHash());

                        try {
                            RouterNode sender = new RouterNode(
                                    new NodeID(message.getOriginHash()),
                                    InetAddress.getByName(message.getOriginIP()),
                                    message.getOriginPort()
                            );
                            // TODO insert?

                            FindNodeActivity findNodeReply = new FindNodeActivity(
                                    this,
                                    this.localNode,
                                    sender,
                                    this.localNode.getRoutingTable(),
                                    message.getFindNodeRequest().getSearchHash()
                            );
                            findNodeReply.setResponse(message.getExchangeID(), message.getFindNodeRequest(), message.getOriginHash());
                            new Thread(() -> findNodeReply.run()).start();

                        } catch (MalformedKeyException e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch (InvalidProtocolBufferException ipbe) {
                    MessageLite ml = ipbe.getUnfinishedMessage();
                    System.out.println();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendDatagram(RouterNode node, KademliaMessage payload, Consumer<KademliaMessage> callback) {
        new Thread(() -> {
            try {
                if(callback != null) this.responses.put(payload.getExchangeID(), callback);
                DatagramSocket socket = new DatagramSocket();
                DatagramPacket packet = new DatagramPacket(payload.toByteArray(), payload.toByteArray().length, node.getIPAddress(), node.getPort());
                socket.send(packet);
                this.print("Sent");
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public InetAddress getLocal() {
        return this.localAddress;
    }

    public int getPort() {
        return this.port;
    }

    private void print(String msg) {
//        System.out.println("["+this.port+"] " + msg);
    }
}
