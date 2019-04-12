package io.hbt.bubblegum.core.kademlia.activities;

import com.google.protobuf.ByteString;
import io.hbt.bubblegum.core.auxiliary.ProtobufHelper;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServerWorker;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaSync.KademliaSync;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.UUID;
import java.util.function.Consumer;

public class SyncActivity extends NetworkActivity {

    private KademliaMessage originalSync;

    public SyncActivity(BubblegumNode self, RouterNode to) {
        super(self, to);
    }


    public void setResponse(KademliaMessage originalSync) {
        super.setResponse(originalSync.getExchangeID());
        if(originalSync.hasSyncMessage() && originalSync.getSyncMessage().getStage() == 1) {
            this.originalSync = originalSync;
        } else {
            this.aborted = true;
        }
    }


    @Override
    public void run() {
        super.run();
        if(this.aborted) {
            this.onFail();
            return;
        }

        // Get real RouterNode if we have one
        RouterNode d = this.routingTable.getRouterNodeForID(this.to.getNode());
        if(d == null) d = this.to;
        final RouterNode destination = d;

        // dest and to with different ip/port?
        byte[] myNonce = UUID.randomUUID().toString().getBytes();
        String label = this.isResponse ? "B" : "A";
        this.print(label + ": Starting");
        this.print(label + ": Nonce - " + new String(myNonce));

        KademliaMessage message = null;
        if(!this.isResponse) {
            // Send stage 1
            byte[] payload = this.localNode.encryptPrivate(myNonce);
            if(payload != null) {
                this.print(label + ": Stage 1 encrypt success");
                message = ProtobufHelper.buildSyncMessage(
                    this.localNode, destination, this.exchangeID,
                    1, this.localNode.getPGPKeyID(), ByteString.copyFrom(payload), ByteString.EMPTY
                );
            } else {
                this.onFail();
                return;
            }
        }
        else {
            // Send stage 2.

            // Get A's public key.
            // Also implicitly checks the IP address for MITM.
            boolean valid = this.localNode.ensurePGPKeyIsLocal(
                originalSync.getSyncMessage().getLabel(),
                KademliaServerWorker.kademliaMessagesToPGPID(originalSync)
            );

            if(!valid) {
                // Suspected MITM / Keyserver down.
                System.err.println(label + ": Key retrieval failed");
                this.onFail();
                return;
            }

            byte[] theirNonce = this.localNode.decryptForNode(
                this.to, this.originalSync.getSyncMessage().getEncryptedA().toByteArray()
            );
            if(theirNonce != null) {
                this.print(label + ": Stage 1 decrypt success");
                theirNonce = this.localNode.encryptPrivate(theirNonce);
                if(theirNonce != null) {
                    this.print(label + ": Stage 2 encrypt N_a success");
                    String payloadAString = this.localNode.getPGPKeyID() + ":" + new String(myNonce);
                    byte[] payloadA = this.localNode.encryptForNode(this.to, payloadAString.getBytes());
                    if(payloadA != null) {
                        this.print(label + ": Stage 2 encrypt payload A success");
                        message = ProtobufHelper.buildSyncMessage(
                            this.localNode, destination, this.exchangeID,
                            2, "", ByteString.copyFrom(payloadA), ByteString.copyFrom(theirNonce)
                        );
                    }
                }
            }
        }


        Consumer<KademliaMessage> response = this.isResponse ? (kademliaMessage -> {
            // Received stage one, sent stage two, and now receiving stage 3.
            this.print(label + ": Received stage 3");
            if(kademliaMessage.getSyncMessage().getStage() == 3) {
                byte[] x = this.localNode.decryptPublic(kademliaMessage.getSyncMessage().getEncryptedA().toByteArray());
                if(x != null) {
                    this.print(label + ": Stage 3 decrypt success");
                    if (new String(x).trim().equals(new String(myNonce))) this.onSuccess();
                    else {
                        this.print(label + ": Stage 3 nonce incorrect");
                        this.onFail();
                    }
                } else {
                    this.onFail();
                }
            } else {
                this.onFail();
            }

        }) : (kademliaMessage -> {
            // Sent stage one, received stage two, now sending stage 3.
            if(kademliaMessage.getSyncMessage().getStage() == 2) {

                byte[] payloadA = this.localNode.decryptPublic(
                    kademliaMessage.getSyncMessage().getEncryptedA().toByteArray()
                );
                if(payloadA != null) {
                    this.print(label + ": Stage 2 decrypt payload A success");
                    String payloadAStr = new String(payloadA);
                    if(payloadAStr.contains(":")) {
                        String[] payloadAParts = payloadAStr.split(":");
                        if(payloadAParts.length == 2) {
                            this.print(label + ": Stage 2 payload A correct ("+payloadAStr+")");
                            // Get B's public key.
                            // Also implicitly checks the IP address for MITM.
                            boolean valid = this.localNode.ensurePGPKeyIsLocal(
                                payloadAParts[0],
                                KademliaServerWorker.kademliaMessagesToPGPID(kademliaMessage)
                            );

                            if (!valid) {
                                // Suspected MITM / Keyserver down.
                                System.err.println(label + ": Key retrieval failed");
                                this.onFail();
                                return;
                            }

                            byte[] payloadB = this.localNode.decryptForNode(
                                this.to, kademliaMessage.getSyncMessage().getEncryptedB().toByteArray()
                            );
                            String returnedNonce = new String(payloadB).trim();

                            if(payloadB != null && returnedNonce.equals(new String(myNonce))) {
                                this.print(label + ": Stage 3 nonce correct");

                                byte[] stage3Payload = this.localNode.encryptForNode(this.to, payloadAParts[1].getBytes());
                                if(stage3Payload != null) {
                                    this.print(label + ": Stage 3 encryption success");

                                    KademliaMessage stage3 = ProtobufHelper.buildSyncMessage(
                                        this.localNode, destination, this.exchangeID,
                                        3, "", ByteString.copyFrom(stage3Payload), ByteString.EMPTY
                                    );
                                    this.server.sendDatagram(localNode, destination, stage3, null);
                                    this.onSuccess();
                                }
                            } else {
                                this.onFail();
                            }
                        } else {
                            this.onFail();
                        }
                    } else {
                        this.onFail();
                    }
                } else {
                    this.onFail();
                }
            } else {
                this.onFail();
            }
        });

//        final NodeID destinationID = destination.getNode();
//        Runnable onTimeout = () -> {
//            RouterNode responder = this.routingTable.getRouterNodeForID(destinationID);
//            if(responder != null) responder.hasFailedToRespond();
//        };


        if(message != null) {
            this.print(label + ": Sent packet");
            this.server.sendDatagram(localNode, destination, message, response);
        }
        else this.onFail();
        this.timeoutOnComplete();
    }

}
