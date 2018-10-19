package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.auxiliary.ConcurrentBlockingQueue;
import io.hbt.bubblegum.core.exceptions.BubblegumException;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class KademliaServer {
    private final BubblegumNode localNode;
    private int port;
    private InetAddress localAddress;
    private final DatagramSocket listeningSocket;

    private final ConcurrentBlockingQueue<DatagramPacket> kademliaHandlers;
    private final int numWorkers = 2;
    private static final int DATAGRAM_BUFFER_SIZE = 64 * 1024; // 64KB

    private final ConcurrentHashMap<String, Consumer<KademliaMessage>> responses = new ConcurrentHashMap<>();

    private boolean alive = false;
    private Thread listenerThread;
    private DatagramSocket sendingSocket;

    private long packetsSent = 0;
    private long packetsReceived = 0;

    public KademliaServer(BubblegumNode local, int port) throws BubblegumException {
        this.localNode = local;
        this.port = port;
        this.kademliaHandlers = new ConcurrentBlockingQueue<>();
        this.initialiseWorkers();

        try {
            this.sendingSocket = new DatagramSocket();
            this.listeningSocket = new DatagramSocket(this.port);
            this.port = this.listeningSocket.getPort();
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

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(
                () -> this.print("Stats  ~  Sent: " + this.packetsSent + ",  Received: " + this.packetsReceived),
                5,
                5,
                TimeUnit.SECONDS
        );
    }

    private void listen() {
        while(this.alive) {
            try {
                byte[] buffer = new byte[DATAGRAM_BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                this.listeningSocket.receive(packet);
                this.kademliaHandlers.put(packet);
                this.packetsReceived++;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initialiseWorkers() {
        Thread t;
        for(int i = 0; i < this.numWorkers; i++) {
            t = new KademliaServerWorker(this.localNode, this.kademliaHandlers, this.responses);
            t.setDaemon(true);
            t.start();
        }
    }

    public void sendDatagram(RouterNode node, KademliaMessage payload, Consumer<KademliaMessage> callback) {
        synchronized (this.sendingSocket) {
            try {
                if (callback != null) this.responses.put(payload.getExchangeID(), callback);

                DatagramPacket packet = new DatagramPacket(payload.toByteArray(), payload.toByteArray().length, node.getIPAddress(), node.getPort());
                if (this.sendingSocket == null || !this.sendingSocket.isConnected() || this.sendingSocket.isClosed()) {
                    this.sendingSocket.close();
                    this.sendingSocket = new DatagramSocket();
                }
                this.sendingSocket.send(packet);
                this.packetsSent++;

            } catch (SocketException e) {
                System.out.println("[Socket Failure] " + e.getMessage());
//                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeCallback(String exchangeID) {
        if(this.responses.containsKey(exchangeID)) this.responses.remove(exchangeID);
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
