package io.hbt.bubblegum.core.kademlia;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import io.hbt.bubblegum.core.auxiliary.ConcurrentBlockingQueue;
import io.hbt.bubblegum.core.exceptions.BubblegumException;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.activities.FindActivity;
import io.hbt.bubblegum.core.kademlia.activities.PingActivity;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
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
    private DatagramSocket sendingSocket;


    public KademliaServer(BubblegumNode local, int port) throws BubblegumException {
        this.localNode = local;
        this.port = port;
        this.kademliaHandlers = new ConcurrentBlockingQueue<>();
        try {
            this.sendingSocket = new DatagramSocket();
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

                // Resize the array to avoid padding issues
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

//                this.kademliaHandlers.put(packet);

                try {
                    KademliaMessage message = KademliaMessage.parseFrom(data);

                    if(this.responses.containsKey(message.getExchangeID())) {
//                        this.print("Callback found for " + message.getExchangeID());
                        Consumer<KademliaMessage> callback = this.responses.remove(message.getExchangeID());
                        if(callback != null) this.localNode.getExecutionContext().addCallbackActivity(
                                this.localNode.getIdentifier().toString(),
                                () -> callback.accept(message)
                        );
                    }

                    else if(message.hasPingMessage()) {
                        this.print("PING Message Received [" + message.getOriginIP() + ":" + message.getOriginPort() + "]");

                        try {
                            RouterNode sender = new RouterNode(
                                    new NodeID(message.getOriginHash()),
                                    InetAddress.getByName(message.getOriginIP()),
                                    message.getOriginPort()
                            );
                            this.localNode.getRoutingTable().insert(sender);

                            PingActivity pingReply = new PingActivity(this, this.localNode, sender, this.localNode.getRoutingTable());
                            pingReply.setResponse(message.getExchangeID());
                            this.localNode.getExecutionContext().addCallbackActivity(this.localNode.getIdentifier().toString(), pingReply);

                        } catch (MalformedKeyException e) {
                            e.printStackTrace();
                        }
                    }

                    else if(message.hasFindNodeRequest()) {
                        boolean returnValue = message.getFindNodeRequest().getReturnValue();
                        this.print((returnValue ? "FIND_VALUE" : "FIND_NODE") + " Request Received [" + message.getOriginIP() + ":" + message.getOriginPort() + "]: " + message.getFindNodeRequest().getSearchHash());

                        try {
                            RouterNode sender = new RouterNode(
                                    new NodeID(message.getOriginHash()),
                                    InetAddress.getByName(message.getOriginIP()),
                                    message.getOriginPort()
                            );
                            // TODO insert?

                            FindActivity findNodeReply = new FindActivity(
                                    this,
                                    this.localNode,
                                    sender,
                                    this.localNode.getRoutingTable(),
                                    message.getFindNodeRequest().getSearchHash(),
                                    returnValue
                            );
                            findNodeReply.setResponse(message.getExchangeID(), message.getFindNodeRequest(), message.getOriginHash());
                            this.localNode.getExecutionContext().addCallbackActivity(this.localNode.getIdentifier().toString(), findNodeReply);

                        } catch (MalformedKeyException e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch (InvalidProtocolBufferException ipbe) {
                    MessageLite ml = ipbe.getUnfinishedMessage();
                    ipbe.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void sendDatagram(RouterNode node, KademliaMessage payload, Consumer<KademliaMessage> callback) {
        try {
            if(callback != null) this.responses.put(payload.getExchangeID(), callback);
            if(this.sendingSocket == null || !this.sendingSocket.isConnected()) {
                this.sendingSocket.close();
                this.sendingSocket = new DatagramSocket();
            }
            DatagramPacket packet = new DatagramPacket(payload.toByteArray(), payload.toByteArray().length, node.getIPAddress(), node.getPort());
            this.sendingSocket.send(packet);

            // this.print("Sent");
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InetAddress getLocal() {
        return this.localAddress;
    }

    public int getPort() {
        return this.port;
    }

    private void print(String msg) {
        this.localNode.log(msg);
    }
}
