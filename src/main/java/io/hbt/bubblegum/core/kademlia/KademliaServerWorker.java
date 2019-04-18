package io.hbt.bubblegum.core.kademlia;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.auxiliary.ProtobufHelper;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.activities.FindActivity;
import io.hbt.bubblegum.core.kademlia.activities.PingActivity;
import io.hbt.bubblegum.core.kademlia.activities.PrimitiveStoreActivity;
import io.hbt.bubblegum.core.kademlia.activities.QueryActivity;
import io.hbt.bubblegum.core.kademlia.activities.ResourceRequestActivity;
import io.hbt.bubblegum.core.kademlia.activities.SyncActivity;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaBinaryPayload.KademliaBinaryPayload;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.net.UnknownHostException;

/**
 * Static KademliaMessage acceptor functions.
 * @author Harri Bell-Thomas, ahb36@cam.ac.uk
 */
public class KademliaServerWorker {

    /**
     * BubblegumNode specific KademliaMessage acceptor.
     * @param node The BubblegumNode recipient.
     * @param message The KademliaMessage received.
     */
    public static void accept(BubblegumNode node, KademliaMessage message) {

        // PING RPC received.
        if(message.hasPingMessage() && !message.getPingMessage().getReply()) {
            RouterNode sender = KademliaServerWorker.messageToRouterNode(message);
            if(sender != null) {
                if(!Configuration.ENABLE_IMPERSONATION_WoT_PROTECTION || !Configuration.ENABLE_PGP) {
                    // SYNC manages this if PGP w. web of trust is enabled.
                    sender = KademliaServerWorker.getFromOriginHash(node, message);
                    node.getRoutingTable().insert(sender);
                }

                PingActivity pingReply = new PingActivity(
                    node,
                    sender,
                    message.getOriginNetwork() + ":" + message.getOriginHash()
                );
                pingReply.setResponse(message);
                node.getExecutionContext().addCallbackActivity(node.getIdentifier(), pingReply);
            }
        }

        // SYNC RPC received.
        else if(message.hasSyncMessage()) {
            if(Configuration.ENABLE_PGP) {
                RouterNode sender = KademliaServerWorker.messageToRouterNode(message);
                if(node.checkForSybilImpersonator(sender.toPGPUID())) {
                    // An impersonator, reject.
                    KademliaMessage rejection = ProtobufHelper.buildSyncMessage(
                        node, sender, message.getExchangeID(),
                        -1, "", ByteString.EMPTY, ByteString.EMPTY
                    );

                    node.getServer().sendDatagram(node, sender, rejection, null);
                    System.out.println("Rejected impersonator from cache.");
                }
                else if (sender != null) {
                    SyncActivity syncActivity = new SyncActivity(node, sender);
                    syncActivity.setResponse(message);
                    node.getExecutionContext().addCallbackActivity(node.getIdentifier(), syncActivity);
                }
            }
        }

        else {
            // These methods are embedded in the binary payload.
            RouterNode sender = KademliaServerWorker.getFromOriginHash(node, message);
            KademliaBinaryPayload binaryPayload = KademliaServerWorker.extractPayload(node, sender, message);
            if(binaryPayload != null) {
                // FIND RPC received.
                if (binaryPayload.hasFindRequest()) {
                    if (sender != null) {
                        node.getRoutingTable().insert(sender);

                        FindActivity findNodeReply = new FindActivity(
                            node,
                            sender,
                            binaryPayload.getFindRequest().getSearchHash(),
                            binaryPayload.getFindRequest().getReturnValue()
                        );
                        findNodeReply.setResponse(message.getExchangeID(), binaryPayload.getFindRequest(), message.getOriginHash());
                        node.getExecutionContext().addCallbackActivity(node.getIdentifier(), findNodeReply);
                    }
                }

                // STORE RPC received.
                else if (binaryPayload.hasStoreRequest()) {
                    if (sender != null) {
                        node.getRoutingTable().insert(sender);

                        PrimitiveStoreActivity storeActivity = new PrimitiveStoreActivity(
                            node,
                            sender,
                            binaryPayload.getStoreRequest().getKey(),
                            binaryPayload.getStoreRequest().getValue().toByteArray()
                        );
                        storeActivity.setResponse(message.getExchangeID());
                        node.getExecutionContext().addCallbackActivity(node.getIdentifier(), storeActivity);
                    }
                }

                // QUERY RPC received.
                else if (binaryPayload.hasQueryRequest()) {
                    if (sender != null) {
                        node.getRoutingTable().insert(sender);

                        QueryActivity queryActivity = new QueryActivity(node, sender, 0, 0, null);
                        queryActivity.setResponse(message.getExchangeID(), binaryPayload.getQueryRequest());
                        node.getExecutionContext().addCallbackActivity(node.getIdentifier(), queryActivity);
                    }
                }

                else if (binaryPayload.hasResourceRequest()) {
                    if (sender != null) {
                        node.getRoutingTable().insert(sender);

                        ResourceRequestActivity resourceRequestActivity = new ResourceRequestActivity(
                            node, sender, binaryPayload.getResourceRequest().getUri()
                        );
                        resourceRequestActivity.setResponse(message.getExchangeID(), message.getOriginIP(), message.getOriginLocal());
                        node.getExecutionContext().addCallbackActivity(node.getIdentifier(), resourceRequestActivity);
                    }
                }
            }
        }
    }

    /**
     * Helper method to retrieve or generate the RouterNode instance for the sender information from a KademliaMessage.
     * @param node The BubblegumNode instance to retrieve/build the RouterNode in the context of.
     * @param message The KademliaMessage object received.
     * @return The generated RouterNode instance or null if the information in the KademliaMessage is invalid.
     */
    public static RouterNode getFromOriginHash(BubblegumNode node, KademliaMessage message) {
        try {
            RouterNode sender = node.getRoutingTable().getRouterNodeForID(new NodeID(message.getOriginHash()));
            if (sender == null) sender = messageToRouterNode(message);
            return sender;
        } catch (MalformedKeyException e) {
            return null;
        }
    }

    public static RouterNode messageToRouterNode(KademliaMessage message) {
        try {
            return new RouterNode(
                new NodeID(message.getOriginHash()),
                NetworkingHelper.getInetAddress(message.getOriginIP()),
                message.getOriginPort()
            );
        } catch (MalformedKeyException e) {
            return null;
        } catch (UnknownHostException e) {
            return null;
        }
    }


    public static KademliaBinaryPayload extractPayload(BubblegumNode node, RouterNode sender, KademliaMessage message) {
        if(message.getPayload() != null) {
            byte[] payload;
            if (Configuration.ENABLE_PGP) {
                payload = node.decryptPacket(sender, message.getPayload());
            } else {
                payload = message.getPayload().getData().toByteArray();
            }

            if(payload != null) {
                try {
                    return KademliaBinaryPayload.parseFrom(payload);
                } catch (InvalidProtocolBufferException e) {
                    System.out.println("Failed to parse");
                    return null;
                }
            } else {
                System.out.println("Packet decryption failed. Dropping and requesting synchronisation....");
                node.requestSync(sender);
            }
        }

        return null;
    }

    public static String kademliaMessagesToPGPID(KademliaMessage message) {
        return String.join(";", message.getOriginIP(), message.getOriginPort()+"", message.getOriginHash());
    }

} // end KademliaServerWorker class