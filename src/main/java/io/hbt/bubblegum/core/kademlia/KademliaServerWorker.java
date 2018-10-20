package io.hbt.bubblegum.core.kademlia;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import io.hbt.bubblegum.core.auxiliary.ConcurrentBlockingQueue;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.activities.FindActivity;
import io.hbt.bubblegum.core.kademlia.activities.PingActivity;
import io.hbt.bubblegum.core.kademlia.activities.PrimitiveStoreActivity;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

class KademliaServerWorker extends Thread {

    private final ConcurrentBlockingQueue<DatagramPacket> queue;
    private final BubblegumNode localNode;
    private final ConcurrentHashMap<String, Consumer<BgKademliaMessage.KademliaMessage>> responses;

    public KademliaServerWorker(BubblegumNode localNode, ConcurrentBlockingQueue<DatagramPacket> queue, ConcurrentHashMap<String, Consumer<BgKademliaMessage.KademliaMessage>> responses) {
        this.queue = queue;
        this.localNode = localNode;
        this.responses = responses;
    }

    @Override
    public void run() {
        while(true) {
            try {
                DatagramPacket packet = queue.get();

                // Resize the array to avoid padding issues
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

                try {
                    BgKademliaMessage.KademliaMessage message = BgKademliaMessage.KademliaMessage.parseFrom(data);

                    if(this.responses.containsKey(message.getExchangeID())) {
//                        this.print("Callback found for " + message.getExchangeID());
                        Consumer<BgKademliaMessage.KademliaMessage> callback = this.responses.remove(message.getExchangeID());
                        if(callback != null) this.localNode.getExecutionContext().addCallbackActivity(
                                this.localNode.getNodeIdentifier().toString(),
                                () -> callback.accept(message)
                        );
                    }

                    else if(message.hasPingMessage() && !message.getPingMessage().getReply()) {
                        this.print("PING received [" + message.getOriginIP() + ":" + message.getOriginPort() + "]");
                        try {
                            RouterNode sender = new RouterNode(
                                    new NodeID(message.getOriginHash()),
                                    InetAddress.getByName(message.getOriginIP()),
                                    message.getOriginPort()
                            );
                            this.localNode.getRoutingTable().insert(sender);

                            PingActivity pingReply = new PingActivity(this.localNode, sender);
                            pingReply.setResponse(message.getExchangeID());
                            this.localNode.getExecutionContext().addCallbackActivity(this.localNode.getNodeIdentifier().toString(), pingReply);

                        } catch (MalformedKeyException e) {
                            e.printStackTrace();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }

                    else if(message.hasFindNodeRequest()) {
                        boolean returnValue = message.getFindNodeRequest().getReturnValue();
                        this.print((returnValue ? "FIND_VALUE" : "FIND_NODE") + " received [" + message.getOriginIP() + ":" + message.getOriginPort() + "]: " + message.getFindNodeRequest().getSearchHash());

                        try {
                            RouterNode sender = new RouterNode(
                                    new NodeID(message.getOriginHash()),
                                    InetAddress.getByName(message.getOriginIP()),
                                    message.getOriginPort()
                            );
                            // TODO insert?

                            FindActivity findNodeReply = new FindActivity(
                                    this.localNode,
                                    sender,
                                    message.getFindNodeRequest().getSearchHash(),
                                    returnValue
                            );
                            findNodeReply.setResponse(message.getExchangeID(), message.getFindNodeRequest(), message.getOriginHash());
                            this.localNode.getExecutionContext().addCallbackActivity(this.localNode.getNodeIdentifier().toString(), findNodeReply);

                        } catch (MalformedKeyException e) {
                            e.printStackTrace();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }

                    else if(message.hasStoreRequest()) {
                        this.print("STORE received[" + message.getOriginIP() + ":" + message.getOriginPort() + "]: " + message.getStoreRequest().getKey());
                        try {
                            RouterNode sender = new RouterNode(
                                    new NodeID(message.getOriginHash()),
                                    InetAddress.getByName(message.getOriginIP()),
                                    message.getOriginPort()
                            );
                            // TODO insert?

                            PrimitiveStoreActivity storeActivity = new PrimitiveStoreActivity(this.localNode, sender, message.getStoreRequest().getKey(), message.getStoreRequest().getValue().toByteArray());
                            storeActivity.setResponse(message.getExchangeID());
                            this.localNode.getExecutionContext().addCallbackActivity(this.localNode.getNodeIdentifier().toString(), storeActivity);

                        } catch (MalformedKeyException e) {
                            e.printStackTrace();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch (InvalidProtocolBufferException ipbe) {
                    MessageLite ml = ipbe.getUnfinishedMessage();
                    ipbe.printStackTrace();
                }

            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void print(String message) {
        this.localNode.log(message);
    }
}