package io.hbt.bubblegum.core.kademlia.activities;

import com.google.protobuf.ByteString;
import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.ProtobufHelper;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.KademliaServerWorker;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage.KademliaMessage;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class SyncActivity extends NetworkActivity {

    private KademliaMessage originalSync;
    private boolean failedWebOfTrustVerification = false;
    private enum WoTVerificationOutcome { PASSED, FAILED, ERROR  }

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

        final RouterNode destination = this.to;

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

            if(Configuration.ENABLE_SYBIL_WEB_OF_TRUST_PROTECTION) {
                WoTVerificationOutcome outcome = this.webOfTrustVerification(destination, this.originalSync);
                switch (outcome) {
                    case PASSED:
                        break;
                    case ERROR:
                    case FAILED:
                        this.onFail();
                        return;
                }
            }

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


        Consumer<KademliaMessage> response = this.isResponse ?
            (resp -> responderSendStageTwo(resp, myNonce, label)) :
            (resp -> originatorSendStageThree(resp, destination, myNonce, label));

        if(message != null) {
            this.print(label + ": Sent packet");
            this.server.sendDatagram(localNode, destination, message, response);
        }
        else this.onFail();
        this.timeoutOnComplete();
    }

    private void responderSendStageTwo(KademliaMessage kademliaMessage, byte[] myNonce, String label) {
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
    }

    private void originatorSendStageThree(KademliaMessage kademliaMessage, RouterNode destination, byte[] myNonce, String label) {
        // Sent stage one, received stage two, now sending stage 3.
        if(kademliaMessage.getSyncMessage().getStage() == 2) {

            // If we're here then we've been accepted into the Web of Trust.
            if(Configuration.ENABLE_SYBIL_WEB_OF_TRUST_PROTECTION) {
                this.localNode.getRoutingTable().insert(this.to);
            }

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
        } else if(kademliaMessage.getSyncMessage().getStage() == -1) {
            // Failed Web of Trust Verification
            this.failedWebOfTrustVerification = true;
            this.onFail();
        } else {
            this.onFail();
        }
    }

    private WoTVerificationOutcome webOfTrustVerification(RouterNode destination, KademliaMessage message) {
        System.out.println("\nPerforming Web of Trust Validation ("+this.localNode.getServer().getPort()+" -> "+destination.toPGPUID()+")");

        // Perform check against own node first.
        if(this.localNode.getNodeIdentifier().equals(destination.getNode())) {
            // Collision detected, reject SYNC.
            KademliaMessage rejection = ProtobufHelper.buildSyncMessage(
                this.localNode, destination, this.exchangeID,
                -1, "", ByteString.EMPTY, ByteString.EMPTY
            );

            this.server.sendDatagram(localNode, destination, rejection, null);
            System.out.println("Failed (Same as Local ID)\n");
            this.localNode.declareSybilImpersonator(destination.toPGPUID());
            return WoTVerificationOutcome.FAILED;
        }

        if(this.localNode.getRoutingTable().getSize() > 1) {

            if(this.localNode.haveKeyForPGPID(destination.toPGPUID())) {
                System.out.println("WoT validation unnecessary, I have " + destination.toPGPUID());
                return WoTVerificationOutcome.PASSED;
            }

            // Perform FIND_NODE to validate unique identity.
            // TODO check for loops here
            LookupActivity lookupActivity = new LookupActivity(this.localNode, this.to.getNode(), 2, false);
            lookupActivity.run();
            if (lookupActivity.getComplete() && lookupActivity.getSuccess()) {
                Set<RouterNode> closest = lookupActivity.getClosestNodes();
                boolean collision = false;
                for (RouterNode routerNode : closest) {
                    if (routerNode.getNode().equals(this.to.getNode())) {
                        if (!routerNode.toPGPUID().equals(destination.toPGPUID())) {
                            System.out.println(routerNode.getIPAddress().getHostAddress() + ":" + routerNode.getPort() + " already has " + routerNode.getNode());
                            collision = true;
                        }
                    }
                }

                if (collision) {
                    // Collision detected, reject SYNC.
                    KademliaMessage rejection = ProtobufHelper.buildSyncMessage(
                        this.localNode, destination, this.exchangeID,
                        -1, "", ByteString.EMPTY, ByteString.EMPTY
                    );

                    this.server.sendDatagram(localNode, destination, rejection, null);
                    System.out.println("Failed\n");
                    this.localNode.declareSybilImpersonator(destination.toPGPUID());
                    return WoTVerificationOutcome.FAILED;

                } else {
                    // Verified, so add it to the Routing Table
                    destination = KademliaServerWorker.getFromOriginHash(this.localNode, message);
                    this.localNode.getRoutingTable().insert(destination);
                    System.out.println("Passed\n");
                    return WoTVerificationOutcome.PASSED;
                }
            } else {
                // Can't verify so fail normally.
                System.out.println("Error\n");
                return WoTVerificationOutcome.ERROR;
            }
        } else {
            // Singleton node, so no we to verify against.
            destination = KademliaServerWorker.getFromOriginHash(this.localNode, message);
            this.localNode.getRoutingTable().insert(destination);
            System.out.println("Passed (Singleton)\n");
            return WoTVerificationOutcome.PASSED;
        }
    }

    public boolean getFailedWebOfTrustVerification() {
        return this.failedWebOfTrustVerification;
    }
}
